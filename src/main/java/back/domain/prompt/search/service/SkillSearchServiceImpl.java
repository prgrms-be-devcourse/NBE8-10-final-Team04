package back.domain.prompt.search.service;

import back.domain.prompt.chunking.service.EmbeddingService;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.search.dto.search.QuerySearchHit;
import back.domain.prompt.search.dto.search.SearchQueryDto;
import back.domain.prompt.search.dto.candidate.CandidateDto;
import back.domain.prompt.search.dto.candidate.CandidateMetadataDto;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.dto.chunk.SkillChunkVectorSearchRowDto;
import back.domain.prompt.search.enums.QueryType;
import back.domain.prompt.search.provider.QueryTypeRuleProvider;
import back.domain.prompt.search.repository.SkillChunkVectorSearchRepository;
import back.domain.prompt.search.util.VectorUtils;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillSearchServiceImpl implements SkillSearchService {

    private final EmbeddingService embeddingService;
    private final SkillChunkVectorSearchRepository skillChunkVectorSearchRepository;
    private final QueryTypeRuleProvider queryTypeRuleProvider;

    private static final int DEFAULT_TOP_K = 30;
    private static final int QUERY_LIMIT = 7;
    private static final int PER_QUERY_TOP_K = DEFAULT_TOP_K * 3;
    // tech 키워드로 간주할 최대 토큰(단어) 수 — 이 수를 초과하면 기능 쿼리로 분류
    private static final int TECH_KEYWORD_MAX_TOKENS = 3;

    // 동일 스킬에 매칭된 쿼리가 2개 이상일 때 초과분 1개당 추가되는 boost
    private static final double MATCHED_QUERY_BOOST_PER_EXTRA = 0.03;
    // 기능(function) 타입 쿼리가 매칭될 때 1개당 추가되는 boost
    private static final double FUNCTION_MATCH_BOOST_PER_QUERY = 0.02;
    // 기술(tech) 타입 쿼리가 매칭될 때 1개당 추가되는 boost
    private static final double TECH_MATCH_BOOST_PER_QUERY = 0.015;
    // alias(spring boot, querydsl, next.js 등) 변형 표기가 문서에 포함될 때 1건당 boost
    private static final double ALIAS_BOOST_PER_MATCH = 0.04;
    // alias boost 합산 상한 — 여러 alias가 동시에 매칭돼도 이 값을 초과하지 않음
    private static final double ALIAS_BOOST_MAX = 0.08;
    // tech 쿼리가 문서에 정확히 포함될 때 1건당 boost
    private static final double EXACT_BOOST_PER_MATCH = 0.05;
    // exact boost 합산 상한
    private static final double EXACT_BOOST_MAX = 0.10;

    // 쿼리 목록을 받아 각 쿼리를 임베딩한 뒤 벡터 검색을 수행하고,
    // 스킬 단위로 결과를 집계하여 최종 점수 기준으로 정렬된 후보 목록을 반환한다.
    @Override
    public SkillChunkSearchResultDto search(List<String> queries) {
        List<SearchQueryDto> searchQueries = toSearchQueryDtos(queries);

        if (searchQueries.isEmpty()) {
            throw new ServiceException(
                    CommonErrorCode.BAD_REQUEST,
                    "[SkillSearchServiceImpl#search] queries is null or empty",
                    "검색 질의는 최소 1개 이상이어야 합니다."
            );
        }

        List<QuerySearchHit> allHits = new ArrayList<>();

        for (SearchQueryDto queryDto : searchQueries) {
            List<Float> queryEmbedding = embeddingService.embed(queryDto.text());
            String queryVector = VectorUtils.toPgVector(queryEmbedding);

            List<SkillChunkVectorSearchRowDto> results =
                    skillChunkVectorSearchRepository.searchTopK(queryVector, PER_QUERY_TOP_K);

            results.forEach(row -> allHits.add(
                    new QuerySearchHit(
                            queryDto.text(),
                            queryDto.type(),
                            row
                    )
            ));
        }

        List<CandidateDto> candidates = allHits.stream()
                .collect(Collectors.groupingBy(hit -> hit.row().skillId()))
                .values().stream()
                .map(this::toGroupedCandidateDto)
                .sorted(Comparator.comparing(CandidateDto::primaryScore).reversed())
                .limit(DEFAULT_TOP_K)
                .toList();

        return new SkillChunkSearchResultDto(candidates);
    }

    // null·공백·중복 쿼리를 제거하고 QUERY_LIMIT 개수만큼 잘라 SearchQueryDto 목록으로 변환한다.
    // 각 쿼리는 정규화 후 QueryType(TECH/FUNCTION)이 분류되어 함께 저장된다.
    private List<SearchQueryDto> toSearchQueryDtos(List<String> queries) {
        if (queries == null) {
            return List.of();
        }

        return queries.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(q -> !q.isBlank())
                .distinct()
                .limit(QUERY_LIMIT)
                .map(query -> new SearchQueryDto(
                        normalizeQuery(query),
                        resolveType(query)
                ))
                .toList();
    }

    // 쿼리를 소문자로 변환하고 하이픈을 공백으로, 연속 공백을 단일 공백으로 정규화한다.
    // 언더바(_)는 임베딩 모델이 단일 토큰으로 인식하도록 의도적으로 유지한다.
    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }

        return query.trim()
                .toLowerCase()
                .replace("-", " ")
                .replaceAll("\\s+", " ");
    }

    // 쿼리를 TECH 또는 FUNCTION 타입으로 분류한다.
    // 화이트리스트 → 기능 힌트 단어 → 기술 키워드 형태 순으로 판별하며,
    // 어느 규칙에도 해당하지 않으면 FUNCTION으로 처리한다.
    private QueryType resolveType(String query) {
        String normalized = normalizeQuery(query);

        if (normalized.isBlank()) {
            return QueryType.FUNCTION;
        }

        if (queryTypeRuleProvider.getTechQueryWhitelist().contains(normalized)) {
            return QueryType.TECH;
        }

        if (containsFunctionHint(normalized)) {
            return QueryType.FUNCTION;
        }

        if (looksLikeTechKeyword(normalized)) {
            return QueryType.TECH;
        }

        return QueryType.FUNCTION;
    }

    // 쿼리에 기능성 힌트 단어(기능, 결제, feature 등)가 포함되어 있으면 true를 반환한다.
    private boolean containsFunctionHint(String normalizedQuery) {
        for (String hint : queryTypeRuleProvider.getFunctionHintWords()) {
            if (normalizedQuery.contains(hint.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // 쿼리가 기술 키워드처럼 생겼는지 판별한다.
    // 토큰이 TECH_KEYWORD_MAX_TOKENS 이하이고 영숫자·기호(. + # _)로만 구성된 경우 TECH로 간주한다.
    private boolean looksLikeTechKeyword(String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return false;
        }

        String[] tokens = normalizedQuery.split("\\s+");

        if (tokens.length > TECH_KEYWORD_MAX_TOKENS) {
            return false;
        }

        return normalizedQuery.matches("^[a-z0-9.+#_ ]+$");
    }

    // 동일 스킬에 대한 히트 목록을 받아 최종 점수를 계산하고 CandidateDto로 변환한다.
    // 최고 유사도를 기준으로 매칭 쿼리 수, 타입별 매칭 수, alias·exact boost를 합산하여 점수를 산출한다.
    private CandidateDto toGroupedCandidateDto(List<QuerySearchHit> groupedHits) {
        SkillChunkVectorSearchRowDto best = groupedHits.stream()
                .map(QuerySearchHit::row)
                .max(Comparator.comparing(SkillChunkVectorSearchRowDto::similarity))
                .orElseThrow();

        double maxSimilarity = groupedHits.stream()
                .map(QuerySearchHit::row)
                .mapToDouble(SkillChunkVectorSearchRowDto::similarity)
                .max()
                .orElseThrow();

        long matchedQueryCount = groupedHits.stream()
                .map(QuerySearchHit::query)
                .distinct()
                .count();

        long functionMatchCount = groupedHits.stream()
                .filter(hit -> hit.queryType() == QueryType.FUNCTION)
                .map(QuerySearchHit::query)
                .distinct()
                .count();

        long techMatchCount = groupedHits.stream()
                .filter(hit -> hit.queryType() == QueryType.TECH)
                .map(QuerySearchHit::query)
                .distinct()
                .count();

        double matchedQueryBoost = Math.max(0, matchedQueryCount - 1) * MATCHED_QUERY_BOOST_PER_EXTRA;
        double functionBoost = functionMatchCount * FUNCTION_MATCH_BOOST_PER_QUERY;
        double techBoost = techMatchCount * TECH_MATCH_BOOST_PER_QUERY;

        String documentText = buildDocumentText(best);

        double aliasBoost = calculateAliasBoost(groupedHits, documentText);
        double exactBoost = calculateExactBoost(groupedHits, documentText);

        double finalScore = maxSimilarity
                + matchedQueryBoost
                + functionBoost
                + techBoost
                + aliasBoost
                + exactBoost;

        return new CandidateDto(
                best.skillId(),
                best.skillName(),
                best.repositoryName(),
                best.repositoryUrl(),
                Category.valueOf(best.category()),
                best.summary() == null || best.summary().isBlank() ? null : best.summary(),
                (float) finalScore,
                new CandidateMetadataDto(
                        best.stars(),
                        best.forks(),
                        best.updatedAt() != null ? best.updatedAt().toString() : null
                )
        );
    }

    // boost 계산에 사용할 문서 텍스트를 스킬명·레포명·요약을 합쳐 소문자로 구성한다.
    private String buildDocumentText(SkillChunkVectorSearchRowDto row) {
        return String.join(" ",
                safe(row.skillName()),
                safe(row.repositoryName()),
                safe(row.summary())
        ).toLowerCase();
    }

    // tech 쿼리의 alias 변형(springboot, node.js 등)이 문서에 포함되면 boost를 부여한다.
    // 임베딩 모델이 표기 변형을 동일 기술로 인식하지 못할 경우를 규칙으로 보완한다.
    private double calculateAliasBoost(List<QuerySearchHit> groupedHits, String documentText) {
        double boost = 0.0;

        List<String> techQueries = groupedHits.stream()
                .filter(hit -> hit.queryType() == QueryType.TECH)
                .map(QuerySearchHit::query)
                .distinct()
                .map(String::toLowerCase)
                .toList();

        var aliasGroups = queryTypeRuleProvider.getAliasGroups();

        for (String techQuery : techQueries) {
            List<String> aliases = aliasGroups.get(techQuery);
            if (aliases != null && containsAny(documentText, aliases.toArray(String[]::new))) {
                boost += ALIAS_BOOST_PER_MATCH;
            }
        }

        return Math.min(boost, ALIAS_BOOST_MAX);
    }

    // tech 쿼리 텍스트가 문서에 정확히 포함될 때 boost를 부여한다.
    private double calculateExactBoost(List<QuerySearchHit> groupedHits, String documentText) {
        double boost = 0.0;

        List<String> techQueries = groupedHits.stream()
                .filter(hit -> hit.queryType() == QueryType.TECH)
                .map(QuerySearchHit::query)
                .distinct()
                .map(String::toLowerCase)
                .toList();

        for (String techQuery : techQueries) {
            if (documentText.contains(techQuery)) {
                boost += EXACT_BOOST_PER_MATCH;
            }
        }

        return Math.min(boost, EXACT_BOOST_MAX);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
