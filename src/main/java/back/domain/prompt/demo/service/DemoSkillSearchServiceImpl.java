package back.domain.prompt.demo.service;

import back.domain.prompt.chunking.service.EmbeddingService;
import back.domain.prompt.demo.dto.DemoChunkVectorSearchRowDto;
import back.domain.prompt.demo.dto.DemoQuerySearchHit;
import back.domain.prompt.demo.repository.DemoSkillChunkVectorSearchRepository;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.search.dto.candidate.CandidateDto;
import back.domain.prompt.search.dto.candidate.CandidateMetadataDto;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.dto.search.SearchQueryDto;
import back.domain.prompt.search.enums.QueryType;
import back.domain.prompt.search.provider.QueryTypeRuleProvider;
import back.domain.prompt.search.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemoSkillSearchServiceImpl implements DemoSkillSearchService {

    private final EmbeddingService embeddingService;
    private final DemoSkillChunkVectorSearchRepository demoSkillChunkVectorSearchRepository;
    private final QueryTypeRuleProvider queryTypeRuleProvider;

    // 데모는 스킬 수가 고정(6개)이므로 항상 전체 후보를 반환한다.
    private static final int DEMO_SKILL_COUNT = 6;
    private static final int QUERY_LIMIT = 7;
    private static final int TECH_KEYWORD_MAX_TOKENS = 3;

    private static final double MATCHED_QUERY_BOOST_PER_EXTRA = 0.03;
    private static final double FUNCTION_MATCH_BOOST_PER_QUERY = 0.02;
    private static final double TECH_MATCH_BOOST_PER_QUERY = 0.015;
    private static final double ALIAS_BOOST_PER_MATCH = 0.04;
    private static final double ALIAS_BOOST_MAX = 0.08;
    private static final double EXACT_BOOST_PER_MATCH = 0.05;
    private static final double EXACT_BOOST_MAX = 0.10;

    // 쿼리 목록을 임베딩한 뒤 demo_skill_chunks 전체를 검색하여
    // 항상 6개 데모 스킬 후보를 점수 순으로 반환한다.
    @Override
    public SkillChunkSearchResultDto search(List<String> queries) {
        List<SearchQueryDto> searchQueries = toSearchQueryDtos(queries);

        if (searchQueries.isEmpty()) {
            throw new IllegalArgumentException("검색 질의는 최소 1개 이상이어야 합니다.");
        }

        List<DemoQuerySearchHit> allHits = new ArrayList<>();

        for (SearchQueryDto queryDto : searchQueries) {
            List<Float> queryEmbedding = embeddingService.embed(queryDto.text());
            String queryVector = VectorUtils.toPgVector(queryEmbedding);

            List<DemoChunkVectorSearchRowDto> results =
                    demoSkillChunkVectorSearchRepository.searchAll(queryVector);

            results.forEach(row -> allHits.add(
                    new DemoQuerySearchHit(queryDto.text(), queryDto.type(), row)
            ));
        }

        List<CandidateDto> candidates = allHits.stream()
                .collect(Collectors.groupingBy(hit -> hit.row().demoSkillId()))
                .values().stream()
                .map(this::toGroupedCandidateDto)
                .sorted(Comparator.comparing(CandidateDto::primaryScore).reversed())
                .limit(DEMO_SKILL_COUNT)
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
                .map(query -> new SearchQueryDto(normalizeQuery(query), resolveType(query)))
                .toList();
    }

    private String normalizeQuery(String query) {
        if (query == null) return "";
        return query.trim().toLowerCase().replace("-", " ").replaceAll("\\s+", " ");
    }

    private QueryType resolveType(String query) {
        String normalized = normalizeQuery(query);
        if (normalized.isBlank()) return QueryType.FUNCTION;
        if (queryTypeRuleProvider.getTechQueryWhitelist().contains(normalized)) return QueryType.TECH;
        if (containsFunctionHint(normalized)) return QueryType.FUNCTION;
        if (looksLikeTechKeyword(normalized)) return QueryType.TECH;
        return QueryType.FUNCTION;
    }

    private boolean containsFunctionHint(String normalizedQuery) {
        for (String hint : queryTypeRuleProvider.getFunctionHintWords()) {
            if (normalizedQuery.contains(hint.toLowerCase())) return true;
        }
        return false;
    }

    private boolean looksLikeTechKeyword(String normalizedQuery) {
        if (normalizedQuery.isBlank()) return false;
        String[] tokens = normalizedQuery.split("\\s+");
        if (tokens.length > TECH_KEYWORD_MAX_TOKENS) return false;
        return normalizedQuery.matches("^[a-z0-9.+#_ ]+$");
    }

    private CandidateDto toGroupedCandidateDto(List<DemoQuerySearchHit> groupedHits) {
        DemoChunkVectorSearchRowDto best = groupedHits.stream()
                .map(DemoQuerySearchHit::row)
                .max(Comparator.comparing(DemoChunkVectorSearchRowDto::similarity))
                .orElseThrow();

        double maxSimilarity = groupedHits.stream()
                .mapToDouble(h -> h.row().similarity())
                .max().orElseThrow();

        long matchedQueryCount = groupedHits.stream()
                .map(DemoQuerySearchHit::query).distinct().count();

        long functionMatchCount = groupedHits.stream()
                .filter(h -> h.queryType() == QueryType.FUNCTION)
                .map(DemoQuerySearchHit::query).distinct().count();

        long techMatchCount = groupedHits.stream()
                .filter(h -> h.queryType() == QueryType.TECH)
                .map(DemoQuerySearchHit::query).distinct().count();

        double matchedQueryBoost = Math.max(0, matchedQueryCount - 1) * MATCHED_QUERY_BOOST_PER_EXTRA;
        double functionBoost = functionMatchCount * FUNCTION_MATCH_BOOST_PER_QUERY;
        double techBoost = techMatchCount * TECH_MATCH_BOOST_PER_QUERY;

        String documentText = buildDocumentText(best);
        double aliasBoost = calculateAliasBoost(groupedHits, documentText);
        double exactBoost = calculateExactBoost(groupedHits, documentText);

        double finalScore = maxSimilarity + matchedQueryBoost + functionBoost + techBoost + aliasBoost + exactBoost + best.boostScore();

        return new CandidateDto(
                best.demoSkillId(),
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

    private String buildDocumentText(DemoChunkVectorSearchRowDto row) {
        return String.join(" ", safe(row.skillName()), safe(row.repositoryName()), safe(row.summary()))
                .toLowerCase();
    }

    private double calculateAliasBoost(List<DemoQuerySearchHit> groupedHits, String documentText) {
        double boost = 0.0;
        var aliasGroups = queryTypeRuleProvider.getAliasGroups();

        List<String> techQueries = groupedHits.stream()
                .filter(h -> h.queryType() == QueryType.TECH)
                .map(DemoQuerySearchHit::query).distinct()
                .map(String::toLowerCase).toList();

        for (String techQuery : techQueries) {
            List<String> aliases = aliasGroups.get(techQuery);
            if (aliases != null && containsAny(documentText, aliases.toArray(String[]::new))) {
                boost += ALIAS_BOOST_PER_MATCH;
            }
        }
        return Math.min(boost, ALIAS_BOOST_MAX);
    }

    private double calculateExactBoost(List<DemoQuerySearchHit> groupedHits, String documentText) {
        double boost = 0.0;

        List<String> techQueries = groupedHits.stream()
                .filter(h -> h.queryType() == QueryType.TECH)
                .map(DemoQuerySearchHit::query).distinct()
                .map(String::toLowerCase).toList();

        for (String techQuery : techQueries) {
            if (documentText.contains(techQuery)) boost += EXACT_BOOST_PER_MATCH;
        }
        return Math.min(boost, EXACT_BOOST_MAX);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
