package back.domain.mcp.recommendation.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;

public record McpRecommendationRequest(
        @NotEmpty(message = "queries-NotEmpty-queries는 최소 1개 이상이어야 합니다.")
        @Size(max = 7, message = "queries-Size-queries는 최대 7개까지 가능합니다.")
        List<
                @NotBlank(message = "queries-NotBlank-query는 공백일 수 없습니다.")
                @Size(max = 100, message = "queries-Size-query는 100자 이하여야 합니다.")
                        String> queries) {

    public McpRecommendationRequest {
        queries = queries == null ? List.of() : List.copyOf(queries);
    }

    @Override
    public List<String> queries() {
        return List.copyOf(queries);
    }
}
