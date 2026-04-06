package back.domain.mcp.recommendation.ranker;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class McpRecommendationRankingLogger {
    private static final Logger log = LoggerFactory.getLogger(McpRecommendationRankingLogger.class);
    private static final int DEBUG_TOP_CANDIDATE_COUNT = 5;

    public void logSkipped() {
        log.info("[McpRecommendationRanker] ranking skipped. candidateCount=0");
    }

    public void logFinished(
            int candidateCount,
            double threshold,
            int passedThresholdCount,
            int rejectedByThresholdCount,
            int categoryWinnerCount,
            int selectedCount) {
        log.info(
                "[McpRecommendationRanker] ranking finished. candidateCount={}, threshold={}, "
                        + "passedThresholdCount={}, rejectedByThresholdCount={}, "
                        + "categoryWinnerCount={}, selectedCount={}",
                candidateCount,
                threshold,
                passedThresholdCount,
                rejectedByThresholdCount,
                categoryWinnerCount,
                selectedCount);
    }

    public void logDebugSummary(
            McpRecommendationRankingProperties rankingProperties,
            McpRecommendationScoringResult scoringResult,
            List<McpScoredCandidate> passedThreshold,
            List<McpScoredCandidate> selectedScored) {
        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug(
                "[McpRecommendationRanker] ranking policy. weights(primary={}, stars={}, forks={}, freshness={}), "
                        + "normalization(maxStarsLog={}, maxForksLog={})",
                rankingProperties.primaryWeight(),
                rankingProperties.starsWeight(),
                rankingProperties.forksWeight(),
                rankingProperties.freshnessWeight(),
                scoringResult.maxStarsLog(),
                scoringResult.maxForksLog());

        logTopCandidates("passed-threshold", passedThreshold);
        logTopCandidates("selected", selectedScored);
    }

    private void logTopCandidates(String label, List<McpScoredCandidate> candidates) {
        List<McpScoredCandidate> topCandidates = candidates.stream()
                .sorted(McpScoredCandidate.ORDER)
                .limit(DEBUG_TOP_CANDIDATE_COUNT)
                .toList();

        for (int index = 0; index < topCandidates.size(); index++) {
            McpScoredCandidate candidate = topCandidates.get(index);
            log.debug(
                    "[McpRecommendationRanker] {}[{}] skillId={}, category={}, finalScore={}, "
                            + "score(primary={}, starsNorm={}, forksNorm={}, freshnessNorm={})",
                    label,
                    index,
                    candidate.skillId(),
                    candidate.category(),
                    candidate.finalScore(),
                    candidate.primaryScore(),
                    candidate.starsNorm(),
                    candidate.forksNorm(),
                    candidate.freshnessNorm());
        }
    }
}
