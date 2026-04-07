package back.domain.mcp.recommendation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record McpRecommendationSkillContentRequest(
        @NotNull(message = "skillId-NotNull-skillId는 필수입니다.")
        @Positive(message = "skillId-Positive-skillId는 1 이상의 값이어야 합니다.")
        Long skillId) {}

