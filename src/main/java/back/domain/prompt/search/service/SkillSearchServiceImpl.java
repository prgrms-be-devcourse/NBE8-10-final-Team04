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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class SkillSearchServiceImpl implements SkillSearchService {

    private final EmbeddingService embeddingService;
    private final SkillChunkVectorSearchRepository skillChunkVectorSearchRepository;
    private final QueryTypeRuleProvider queryTypeRuleProvider;
    private final Executor skillSearchExecutor;

    // @RequiredArgsConstructor 대신 명시적 생성자 — @Qualifier 는 필드가 아닌 파라미터에 적용해야 동작함
    public SkillSearchServiceImpl(
            EmbeddingService embeddingService,
            SkillChunkVectorSearchRepository skillChunkVectorSearchRepository,
            QueryTypeRuleProvider queryTypeRuleProvider,
            @Qualifier("skillSearchExecutor") Executor skillSearchExecutor) {
        this.embeddingService = embeddingService;
        this.skillChunkVectorSearchRepository = skillChunkVectorSearchRepository;
        this.queryTypeRuleProvider = queryTypeRuleProvider;
        this.skillSearchExecutor = skillSearchExecutor;
    }

    private static final int DEFAULT_TOP_K = 30;
    private static final int QUERY_LIMIT = 7;
    private static final int PER_QUERY_TOP_K = DEFAULT_TOP_K * 3;
    private static final int TECH_KEYWORD_MAX_TOKENS = 3;

    private static final double MATCHED_QUERY_BOOST_PER_EXTRA = 0.03;
    private static final double FUNCTION_MATCH_BOOST_PER_QUERY = 0.02;
    private static final double TECH_MATCH_BOOST_PER_QUERY = 0.015;
    private static final double ALIAS_BOOST_PER_MATCH = 0.04;
    private static final double ALIAS_BOOST_MAX = 0.08;
    private static final double EXACT_BOOST_PER_MATCH = 0.05;
    private static final double EXACT_BOOST_MAX = 0.10;

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

        // Phase 1: embed 병렬 실행 (Virtual Thread, DB 연결 없음)
        // Virtual Thread 를 embed 에만 적용하는 이유:
        //   embed = 외부 HTTP I/O → Virtual Thread 효과 있음
        //   searchTopK = DB 쿼리 → DB 연결 대기는 JVM 레벨 락(Pinning)이라
        //   Virtual Thread 를 쓰면 100 VU × 7 = 700개 동시 DB 연결 → HikariPool 고갈
        //   따라서 DB 쿼리는 Phase 2 에서 순차 실행으로 분리
        List<CompletableFuture<String>> embedFutures = searchQueries.stream()
                .map(queryDto -> CompletableFuture.supplyAsync(
                        () -> VectorUtils.toPgVector(embeddingService.embed(queryDto.text())),
                        skillSearchExecutor
                ))
                .toList();

        List<String> queryVectors;
        try {
            queryVectors = embedFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServiceException se) {
                throw se;
            }
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[SkillSearchServiceImpl#search] parallel embedding failed: " + e.getMessage(),
                    "검색 중 오류가 발생했습니다."
            );
        }

        // Phase 2: 벡터 검색 순차 실행 (DB 연결 최대 1개씩 사용)
        // 7 × db_query_latency ≈ 7 × 20ms = 140ms — embed 병목 대비 무시할 수준
        List<QuerySearchHit> allHits = new ArrayList<>();
        for (int i = 0; i < searchQueries.size(); i++) {
            SearchQueryDto queryDto = searchQueries.get(i);
            String queryVector = queryVectors.get(i);
            skillChunkVectorSearchRepository.searchTopK(queryVector, PER_QUERY_TOP_K)
                    .forEach(row -> allHits.add(
                            new QuerySearchHit(queryDto.text(), queryDto.type(), row)
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

    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }

        return query.trim()
                .toLowerCase()
                .replace("-", " ")
                .replaceAll("\\s+", " ");
    }

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

    private boolean containsFunctionHint(String normalizedQuery) {
        for (String hint : queryTypeRuleProvider.getFunctionHintWords()) {
            if (normalizedQuery.contains(hint.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

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

    private String buildDocumentText(SkillChunkVectorSearchRowDto row) {
        return String.join(" ",
                safe(row.skillName()),
                safe(row.repositoryName()),
                safe(row.summary())
        ).toLowerCase();
    }

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
