package back.domain.mcp.recommendation.ranker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.mcp.recommendation.ranking")
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "ConfigurationProperties 바인딩용 객체이며 스프링 컨테이너 관리 하에 사용됩니다.")
public class McpRecommendationRankingProperties {
    private Weight weight = new Weight();
    private double finalScoreThreshold = 0.45;

    public double primaryWeight() {
        return weight.getPrimary();
    }

    public double starsWeight() {
        return weight.getStars();
    }

    public double forksWeight() {
        return weight.getForks();
    }

    public double freshnessWeight() {
        return weight.getFreshness();
    }

    @Getter
    @Setter
    public static class Weight {
        private double primary = 0.60;
        private double stars = 0.20;
        private double forks = 0.10;
        private double freshness = 0.10;
    }
}
