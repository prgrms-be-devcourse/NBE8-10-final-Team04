package back.domain.mcp.recommendation.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import back.domain.prompt.demo.entity.DemoSkill;
import back.domain.prompt.demo.repository.DemoSkillRepository;
import back.domain.prompt.prompt.dto.SkillContentDetail;
import back.domain.prompt.prompt.service.SkillReadService;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class McpRecommendationServiceImpl implements McpRecommendationService {
    private static final Logger log = LoggerFactory.getLogger(McpRecommendationServiceImpl.class);
    private static final String DEMO_SOURCE = "demo-skill-search";
    private static final String DEMO_SKILL_NOT_FOUND_MESSAGE = "데모 스킬이 존재하지 않습니다.";

    private final McpRecommendationCandidateProvider mcpRecommendationCandidateProvider;
    private final McpRecommendationRanker mcpRecommendationRanker;
    private final SkillReadService skillReadService;
    private final DemoSkillRepository demoSkillRepository;

    @Value("${app.mcp.recommendation.candidate-source:skill-search}")
    private String candidateSource;

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
        if (DEMO_SOURCE.equalsIgnoreCase(candidateSource)) {
            DemoSkill demoSkill = demoSkillRepository.findById(request.skillId())
                    .orElseThrow(() -> new ServiceException(
                            CommonErrorCode.NOT_FOUND,
                            "[McpRecommendationServiceImpl#getSkillContent] demo skill not found. skillId=%d"
                                    .formatted(request.skillId()),
                            DEMO_SKILL_NOT_FOUND_MESSAGE));

            return new McpRecommendationSkillContentResponse(
                    demoSkill.getId(),
                    demoSkill.getCategory().name().toLowerCase(),
                    demoSkill.getRepositoryName(),
                    demoSkill.getContentMd());
        }

        SkillContentDetail skillContent = skillReadService.getSkillContent(request.skillId());
        return new McpRecommendationSkillContentResponse(
                skillContent.skillId(),
                skillContent.category(),
                skillContent.sourceRepo(),
                skillContent.contentMd());
    }
}
