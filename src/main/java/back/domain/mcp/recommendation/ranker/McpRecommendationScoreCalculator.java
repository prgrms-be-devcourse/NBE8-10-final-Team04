package back.domain.mcp.recommendation.ranker;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;

@Component
public class McpRecommendationScoreCalculator {
    private static final Logger log = LoggerFactory.getLogger(McpRecommendationScoreCalculator.class);

    private static final double FRESHNESS_DECAY_DAYS = 180.0;
    private static final String UNCATEGORIZED = "uncategorized";

    public McpRecommendationScoringResult calculate(
            List<McpRecommendationCandidate> candidates,
            McpRecommendationRankingProperties rankingProperties) {
        double maxStarsLog = candidates.stream()
                .mapToDouble(candidate -> Math.log1p(extractStars(candidate)))
                .max()
                .orElse(0.0);
        double maxForksLog = candidates.stream()
                .mapToDouble(candidate -> Math.log1p(extractForks(candidate)))
                .max()
                .orElse(0.0);

        List<McpScoredCandidate> scoredCandidates = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            McpRecommendationCandidate candidate = candidates.get(index);
            McpScoredCandidate scoredCandidate =
                    scoreCandidate(candidate, maxStarsLog, maxForksLog, rankingProperties);
            scoredCandidates.add(scoredCandidate);

            if (log.isTraceEnabled()) {
                logScoringTrace(index, candidate, scoredCandidate);
            }
        }

        return new McpRecommendationScoringResult(scoredCandidates, maxStarsLog, maxForksLog);
    }

    private McpScoredCandidate scoreCandidate(
            McpRecommendationCandidate candidate,
            double maxStarsLog,
            double maxForksLog,
            McpRecommendationRankingProperties rankingProperties) {
        double primaryScore = normalizePrimaryScore(candidate.primaryScore());
        int stars = extractStars(candidate);
        int forks = extractForks(candidate);

        double starsNorm = normalizeLogMetric(stars, maxStarsLog);
        double forksNorm = normalizeLogMetric(forks, maxForksLog);
        double freshnessNorm = calculateFreshnessNorm(candidate);
        double finalScore = (rankingProperties.primaryWeight() * primaryScore)
                + (rankingProperties.starsWeight() * starsNorm)
                + (rankingProperties.forksWeight() * forksNorm)
                + (rankingProperties.freshnessWeight() * freshnessNorm);

        return new McpScoredCandidate(
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

    private void logScoringTrace(int index, McpRecommendationCandidate source, McpScoredCandidate scored) {
        String updatedAt = source.metadata() == null ? null : source.metadata().updatedAt();

        log.trace(
                "[McpRecommendationScoreCalculator] candidate[{}] skillId={}, category={}, primaryRaw={}, "
                        + "primaryNorm={}, stars={}, starsNorm={}, forks={}, forksNorm={}, updatedAt={}, "
                        + "freshnessNorm={}, finalScore={}",
                index,
                scored.skillId(),
                scored.category(),
                source.primaryScore(),
                scored.primaryScore(),
                scored.stars(),
                scored.starsNorm(),
                scored.forks(),
                scored.forksNorm(),
                updatedAt,
                scored.freshnessNorm(),
                scored.finalScore());
    }
}
