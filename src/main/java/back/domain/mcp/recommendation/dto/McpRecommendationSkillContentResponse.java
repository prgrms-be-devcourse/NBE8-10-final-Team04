package back.domain.mcp.recommendation.dto;

public record McpRecommendationSkillContentResponse(
        long skillId,
        String category,
        String sourceRepo,
        String skillMdRaw) {}

