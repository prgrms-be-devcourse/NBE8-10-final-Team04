package back.domain.mcp.recommendation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.candidate.dto.McpRecommendationQuery;
import back.domain.mcp.candidate.provider.McpRecommendationCandidateProvider;
import back.domain.mcp.recommendation.dto.McpRecommendationRequest;
import back.domain.mcp.recommendation.dto.McpRecommendationResponse;
import back.domain.mcp.recommendation.dto.McpRecommendedSkillResponse;
import back.domain.mcp.recommendation.ranker.McpRecommendationRanker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class McpRecommendationServiceImpl implements McpRecommendationService {
    private final McpRecommendationCandidateProvider mcpRecommendationCandidateProvider;
    private final McpRecommendationRanker mcpRecommendationRanker;

    @Override
    public McpRecommendationResponse recommend(McpRecommendationRequest request) {
        McpRecommendationQuery query = new McpRecommendationQuery(request.keywords());

        List<McpRecommendationCandidate> candidates = mcpRecommendationCandidateProvider.findTopCandidates(query);
        List<McpRecommendedSkillResponse> selectedSkills = mcpRecommendationRanker.rank(candidates);
        return new McpRecommendationResponse(selectedSkills);
    }
}
