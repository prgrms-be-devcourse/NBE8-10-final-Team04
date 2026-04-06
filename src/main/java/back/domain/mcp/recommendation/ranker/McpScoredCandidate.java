package back.domain.mcp.recommendation.ranker;

import java.util.Comparator;

import back.domain.mcp.recommendation.dto.McpRecommendedSkillResponse;
import back.domain.mcp.recommendation.dto.McpRecommendationScoreBreakdown;

public record McpScoredCandidate(
        long skillId,
        String category,
        double finalScore,
        double primaryScore,
        double starsNorm,
        double forksNorm,
        double freshnessNorm,
        int stars,
        int forks,
        String sourceRepo,
        String skillMdRaw) {

    public static final Comparator<McpScoredCandidate> ORDER = Comparator
            .comparingDouble(McpScoredCandidate::finalScore)
            .reversed()
            .thenComparing(Comparator.comparingDouble(McpScoredCandidate::primaryScore).reversed())
            .thenComparing(Comparator.comparingDouble(McpScoredCandidate::freshnessNorm).reversed())
            .thenComparing(Comparator.comparingInt(McpScoredCandidate::stars).reversed())
            .thenComparing(Comparator.comparingInt(McpScoredCandidate::forks).reversed());

    public McpRecommendedSkillResponse toResponse() {
        return new McpRecommendedSkillResponse(
                skillId,
                category,
                finalScore,
                new McpRecommendationScoreBreakdown(primaryScore, starsNorm, forksNorm, freshnessNorm),
                sourceRepo,
                skillMdRaw);
    }
}
