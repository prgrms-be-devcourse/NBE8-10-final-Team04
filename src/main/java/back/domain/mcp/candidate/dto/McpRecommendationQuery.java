package back.domain.mcp.candidate.dto;

import java.util.List;
import java.util.Objects;

public record McpRecommendationQuery(List<String> queries) {
    public McpRecommendationQuery {
        queries = normalize(queries);
    }

    @Override
    public List<String> queries() {
        return List.copyOf(queries);
    }

    private static List<String> normalize(List<String> rawQueries) {
        if (rawQueries == null) {
            return List.of();
        }

        return rawQueries.stream()
                .filter(Objects::nonNull)
                .map(rawQuery -> rawQuery.trim().replaceAll("\\s+", " "))
                .filter(query -> !query.isBlank())
                .distinct()
                .toList();
    }
}
