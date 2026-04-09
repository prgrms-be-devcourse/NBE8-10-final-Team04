package back.domain.mcp.recommendation.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.candidate.dto.McpRecommendationQuery;
import back.domain.mcp.candidate.provider.McpRecommendationCandidateProvider;
import back.domain.mcp.recommendation.dto.McpRecommendationRequest;
import back.domain.mcp.recommendation.dto.McpRecommendationResponse;
import back.domain.mcp.recommendation.dto.McpRecommendationSkillContentRequest;
import back.domain.mcp.recommendation.dto.McpRecommendationSkillContentResponse;
import back.domain.mcp.recommendation.dto.McpRecommendedSkillResponse;
import back.domain.mcp.recommendation.ranker.McpRecommendationRanker;
import back.domain.prompt.prompt.dto.SkillContentDetail;
import back.domain.prompt.prompt.service.SkillReadService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class McpRecommendationServiceImpl implements McpRecommendationService {
    private static final Logger log = LoggerFactory.getLogger(McpRecommendationServiceImpl.class);

    private final McpRecommendationCandidateProvider mcpRecommendationCandidateProvider;
    private final McpRecommendationRanker mcpRecommendationRanker;
    private final SkillReadService skillReadService;

    @Override
    public McpRecommendationResponse recommend(McpRecommendationRequest request) {
        log.debug("[McpRecommendationService] recommend start. queries={}", request.queries());

        McpRecommendationQuery query = new McpRecommendationQuery(request.queries());

        List<McpRecommendationCandidate> candidates = mcpRecommendationCandidateProvider.findTopCandidates(query);
        log.debug("[McpRecommendationService] candidate fetch done. candidateCount={}", candidates.size());

        List<McpRecommendedSkillResponse> selectedSkills = mcpRecommendationRanker.rank(candidates);
        log.debug("[McpRecommendationService] ranking done. selectedCount={}", selectedSkills.size());

        return new McpRecommendationResponse(selectedSkills);
    }

    @Override
    public McpRecommendationSkillContentResponse getSkillContent(McpRecommendationSkillContentRequest request) {
        SkillContentDetail skillContent = skillReadService.getSkillContent(request.skillId());
        return new McpRecommendationSkillContentResponse(
                skillContent.skillId(),
                skillContent.category(),
                skillContent.sourceRepo(),
                skillContent.contentMd());
    }
}
