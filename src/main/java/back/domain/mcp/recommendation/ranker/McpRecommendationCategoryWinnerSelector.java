package back.domain.mcp.recommendation.ranker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class McpRecommendationCategoryWinnerSelector {
    private static final Logger log = LoggerFactory.getLogger(McpRecommendationCategoryWinnerSelector.class);

    public Map<String, McpScoredCandidate> selectBestByCategory(List<McpScoredCandidate> candidates) {
        Map<String, McpScoredCandidate> bestByCategory = new HashMap<>();

        for (McpScoredCandidate candidate : candidates) {
            McpScoredCandidate previous = bestByCategory.get(candidate.category());
            if (previous == null) {
                bestByCategory.put(candidate.category(), candidate);
                if (log.isTraceEnabled()) {
                    log.trace(
                            "[McpCategoryWinnerSelector] category winner selected. "
                                    + "category={}, skillId={}, finalScore={}",
                            candidate.category(),
                            candidate.skillId(),
                            candidate.finalScore());
                }
                continue;
            }

            McpScoredCandidate winner = pickHigher(previous, candidate);
            bestByCategory.put(candidate.category(), winner);
            if (log.isTraceEnabled() && winner != previous) {
                log.trace(
                        "[McpCategoryWinnerSelector] category winner replaced. category={}, prevSkillId={}, "
                                + "prevFinalScore={}, newSkillId={}, newFinalScore={}",
                        candidate.category(),
                        previous.skillId(),
                        previous.finalScore(),
                        winner.skillId(),
                        winner.finalScore());
            }
        }

        return bestByCategory;
    }

    private McpScoredCandidate pickHigher(McpScoredCandidate left, McpScoredCandidate right) {
        return McpScoredCandidate.ORDER.compare(left, right) <= 0 ? left : right;
    }
}
