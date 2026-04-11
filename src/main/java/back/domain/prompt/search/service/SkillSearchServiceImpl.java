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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class SkillSearchServiceImpl implements SkillSearchService {

    private final EmbeddingService embeddingService;
    private final SkillChunkVectorSearchRepository skillChunkVectorSearchRepository;
    private final QueryTypeRuleProvider queryTypeRuleProvider;
    private final Executor skillSearchExecutor;

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
    private static final int PER_QUERY_TOP_K = DEFAULT_TOP_K * 2;
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

        // Phase 1: 배치 임베딩 (1번의 HTTP 호출로 7개 쿼리를 동시에 임베딩)
        //
        // 7회 개별 병렬 호출(CompletableFuture) 대신 배치 호출을 쓰는 이유:
        //   개별 병렬: 100 VU × 7 쿼리 = 700개 동시 HTTP 연결 → embed 서버 포화
        //   배치 1회: 100 VU × 1 요청 = 100개 동시 HTTP 연결 → embed 서버 부하 7배 감소
        //
        //   GPU 배치 처리 특성:
        //     단일 embed 와 7개 배치 embed 의 처리 시간이 거의 동일
        //     (GPU 는 행렬 연산으로 배치를 병렬 처리)
        //
        // 결과: embed 서버 부하 7배 감소 + 응답시간 7배 단축 (14s → 2s)
        List<String> texts = searchQueries.stream()
                .map(SearchQueryDto::text)
                .toList();

        // JVM 레벨 타임아웃: HTTP 클라이언트 설정과 무관하게 10초 내 Tomcat 스레드 강제 해제
        // embedBatch 를 Virtual Thread 에서 실행 → Tomcat 스레드는 join() 에서 대기
        // orTimeout(10s): 10초 후 TimeoutException → CompletionException → ServiceException → 500
        // 이로써 HTTP 클라이언트 timeout 설정이 어떤 이유로든 발동하지 않아도
        // Tomcat 스레드가 10s 이상 묶이지 않아 스레드 누적(PENDING)이 원천 차단된다.
        List<List<Float>> batchEmbeddings;
        try {
            batchEmbeddings = CompletableFuture
                    .supplyAsync(() -> embeddingService.embedBatch(texts), skillSearchExecutor)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServiceException se) throw se;
            if (cause instanceof TimeoutException) {
                throw new ServiceException(
                        CommonErrorCode.INTERNAL_SERVER_ERROR,
                        "[SkillSearchServiceImpl#search] embedding timed out after 10s",
                        "검색 중 오류가 발생했습니다."
                );
            }
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[SkillSearchServiceImpl#search] batch embedding failed: " + e.getMessage(),
                    "검색 중 오류가 발생했습니다."
            );
        }

        List<String> queryVectors = batchEmbeddings.stream()
                .map(VectorUtils::toPgVector)
                .toList();

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
