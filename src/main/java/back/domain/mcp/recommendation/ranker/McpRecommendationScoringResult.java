package back.domain.mcp.recommendation.ranker;

import java.util.List;

public record McpRecommendationScoringResult(
        List<McpScoredCandidate> scoredCandidates,
        double maxStarsLog,
        double maxForksLog) {
    public McpRecommendationScoringResult {
        scoredCandidates = scoredCandidates == null ? List.of() : List.copyOf(scoredCandidates);
    }
}
