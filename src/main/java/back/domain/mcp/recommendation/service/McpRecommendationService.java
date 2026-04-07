package back.domain.mcp.recommendation.service;

import back.domain.mcp.recommendation.dto.McpRecommendationRequest;
import back.domain.mcp.recommendation.dto.McpRecommendationResponse;
import back.domain.mcp.recommendation.dto.McpRecommendationSkillContentRequest;
import back.domain.mcp.recommendation.dto.McpRecommendationSkillContentResponse;

public interface McpRecommendationService {
    McpRecommendationResponse recommend(McpRecommendationRequest request);

    McpRecommendationSkillContentResponse getSkillContent(McpRecommendationSkillContentRequest request);
}
