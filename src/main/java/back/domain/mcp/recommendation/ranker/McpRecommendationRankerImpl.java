package back.domain.mcp.recommendation.ranker;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.recommendation.dto.McpRecommendedSkillResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

@Component
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "ConfigurationProperties 빈 참조를 읽기 전용으로 주입받아 사용합니다.")
@RequiredArgsConstructor
public class McpRecommendationRankerImpl implements McpRecommendationRanker {
    private static final int MAX_SELECTED_SKILLS = 10;

    private final McpRecommendationScoreCalculator scoreCalculator;
    private final McpRecommendationCategoryWinnerSelector categoryWinnerSelector;
    private final McpRecommendationRankingLogger rankingLogger;
    private final McpRecommendationRankingProperties rankingProperties;

    @Override
    public List<McpRecommendedSkillResponse> rank(List<McpRecommendationCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            rankingLogger.logSkipped();
            return List.of();
        }

        McpRecommendationScoringResult scoringResult = scoreCalculator.calculate(candidates, rankingProperties);
        List<McpScoredCandidate> scoredCandidates = scoringResult.scoredCandidates();
        double finalScoreThreshold = rankingProperties.getFinalScoreThreshold();

        List<McpScoredCandidate> passedThreshold = scoredCandidates.stream()
                .filter(candidate -> candidate.finalScore() >= finalScoreThreshold)
                .toList();

        Map<String, McpScoredCandidate> bestByCategory = categoryWinnerSelector.selectBestByCategory(passedThreshold);
        List<McpScoredCandidate> selectedScored = bestByCategory.values().stream()
                .sorted(McpScoredCandidate.ORDER)
                .limit(MAX_SELECTED_SKILLS)
                .toList();

        List<McpRecommendedSkillResponse> selected = selectedScored.stream()
                .map(McpScoredCandidate::toResponse)
                .toList();

        int rejectedByThresholdCount = scoredCandidates.size() - passedThreshold.size();
        rankingLogger.logFinished(
                scoredCandidates.size(),
                finalScoreThreshold,
                passedThreshold.size(),
                rejectedByThresholdCount,
                bestByCategory.size(),
                selected.size());

        rankingLogger.logDebugSummary(rankingProperties, scoringResult, passedThreshold, selectedScored);
        return selected;
    }
}
