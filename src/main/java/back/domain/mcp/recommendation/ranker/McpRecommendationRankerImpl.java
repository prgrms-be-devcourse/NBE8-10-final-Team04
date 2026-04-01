package back.domain.mcp.recommendation.ranker;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.recommendation.dto.McpRecommendedSkillResponse;
import back.domain.mcp.recommendation.dto.McpRecommendationScoreBreakdown;

@Component
public class McpRecommendationRankerImpl implements McpRecommendationRanker {
    private static final double PRIMARY_SCORE_WEIGHT = 0.60;
    private static final double STARS_WEIGHT = 0.20;
    private static final double FORKS_WEIGHT = 0.10;
    private static final double FRESHNESS_WEIGHT = 0.10;

    private static final double FRESHNESS_DECAY_DAYS = 180.0;
    private static final double FINAL_SCORE_THRESHOLD = 0.45;
    private static final int MAX_SELECTED_SKILLS = 10;
    private static final String UNCATEGORIZED = "uncategorized";

    private static final Comparator<ScoredCandidate> CANDIDATE_ORDER = Comparator
            .comparingDouble(ScoredCandidate::finalScore).reversed()
            .thenComparingDouble(ScoredCandidate::primaryScore).reversed()
            .thenComparingDouble(ScoredCandidate::freshnessNorm).reversed()
            .thenComparingInt(ScoredCandidate::stars).reversed()
            .thenComparingInt(ScoredCandidate::forks).reversed();

    @Override
    public List<McpRecommendedSkillResponse> rank(List<McpRecommendationCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        double maxStarsLog = candidates.stream()
                .mapToDouble(candidate -> Math.log1p(extractStars(candidate)))
                .max()
                .orElse(0.0);
        double maxForksLog = candidates.stream()
                .mapToDouble(candidate -> Math.log1p(extractForks(candidate)))
                .max()
                .orElse(0.0);

        Map<String, ScoredCandidate> bestByCategory = new HashMap<>();
        for (McpRecommendationCandidate candidate : candidates) {
            ScoredCandidate scoredCandidate = scoreCandidate(candidate, maxStarsLog, maxForksLog);
            if (scoredCandidate.finalScore() < FINAL_SCORE_THRESHOLD) {
                continue;
            }

            bestByCategory.merge(scoredCandidate.category(), scoredCandidate, this::pickHigher);
        }

        return bestByCategory.values().stream()
                .sorted(CANDIDATE_ORDER)
                .limit(MAX_SELECTED_SKILLS)
                .map(ScoredCandidate::toResponse)
                .toList();
    }

    private ScoredCandidate scoreCandidate(
            McpRecommendationCandidate candidate, double maxStarsLog, double maxForksLog) {
        double primaryScore = normalizePrimaryScore(candidate.primaryScore());
        int stars = extractStars(candidate);
        int forks = extractForks(candidate);

        double starsNorm = normalizeLogMetric(stars, maxStarsLog);
        double forksNorm = normalizeLogMetric(forks, maxForksLog);
        double freshnessNorm = calculateFreshnessNorm(candidate);
        double finalScore = (PRIMARY_SCORE_WEIGHT * primaryScore)
                + (STARS_WEIGHT * starsNorm)
                + (FORKS_WEIGHT * forksNorm)
                + (FRESHNESS_WEIGHT * freshnessNorm);

        return new ScoredCandidate(
                candidate.skillId() == null ? 0L : candidate.skillId(),
                normalizeCategory(candidate.category()),
                finalScore,
                primaryScore,
                starsNorm,
                forksNorm,
                freshnessNorm,
                stars,
                forks,
                resolveSourceRepo(candidate),
                resolveSkillMdRaw(candidate));
    }

    private ScoredCandidate pickHigher(ScoredCandidate left, ScoredCandidate right) {
        return CANDIDATE_ORDER.compare(left, right) <= 0 ? left : right;
    }

    private double normalizePrimaryScore(Double primaryScore) {
        if (primaryScore == null) {
            return 0.0;
        }
        if (primaryScore < 0.0) {
            return 0.0;
        }
        if (primaryScore > 1.0) {
            return 1.0;
        }
        return primaryScore;
    }

    private int extractStars(McpRecommendationCandidate candidate) {
        if (candidate.metadata() == null || candidate.metadata().stars() == null) {
            return 0;
        }
        return Math.max(candidate.metadata().stars(), 0);
    }

    private int extractForks(McpRecommendationCandidate candidate) {
        if (candidate.metadata() == null || candidate.metadata().forks() == null) {
            return 0;
        }
        return Math.max(candidate.metadata().forks(), 0);
    }

    private double normalizeLogMetric(int rawValue, double maxLogValue) {
        if (maxLogValue <= 0.0) {
            return 0.0;
        }
        return Math.log1p(rawValue) / maxLogValue;
    }

    private double calculateFreshnessNorm(McpRecommendationCandidate candidate) {
        if (candidate.metadata() == null || candidate.metadata().updatedAt() == null) {
            return 0.0;
        }

        try {
            OffsetDateTime updatedAt = OffsetDateTime.parse(candidate.metadata().updatedAt());
            long ageDays = ChronoUnit.DAYS.between(updatedAt.toLocalDate(), OffsetDateTime.now().toLocalDate());
            if (ageDays < 0) {
                ageDays = 0;
            }
            return Math.exp(-(ageDays / FRESHNESS_DECAY_DAYS));
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return UNCATEGORIZED;
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveSourceRepo(McpRecommendationCandidate candidate) {
        String repositoryUrl = candidate.repositoryUrl();
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return candidate.repositoryName() == null ? "" : candidate.repositoryName();
        }

        try {
            URI uri = URI.create(repositoryUrl);
            String[] segments = uri.getPath().split("/");
            if (segments.length >= 3) {
                String owner = segments[1];
                String repo = segments[2].replace(".git", "");
                return owner + "/" + repo;
            }
        } catch (RuntimeException ignored) {
            // URL 파싱 실패 시 repository_name으로 fallback
        }

        return candidate.repositoryName() == null ? "" : candidate.repositoryName();
    }

    private String resolveSkillMdRaw(McpRecommendationCandidate candidate) {
        if (candidate.contentMd() != null && !candidate.contentMd().isBlank()) {
            return candidate.contentMd();
        }

        if (candidate.summary() != null && !candidate.summary().isBlank()) {
            return candidate.summary();
        }

        return "";
    }

    private record ScoredCandidate(
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
        private McpRecommendedSkillResponse toResponse() {
            return new McpRecommendedSkillResponse(
                    skillId,
                    category,
                    finalScore,
                    new McpRecommendationScoreBreakdown(primaryScore, starsNorm, forksNorm, freshnessNorm),
                    sourceRepo,
                    skillMdRaw);
        }
    }
}
