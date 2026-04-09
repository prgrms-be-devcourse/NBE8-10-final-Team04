package back.domain.mcp.candidate.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class McpRecommendationQueryTest {

    @Test
    @DisplayName("queries는 null이면 빈 리스트로 정규화한다")
    void normalize_whenNull() {
        McpRecommendationQuery query = new McpRecommendationQuery(null);

        assertThat(query.queries()).isEmpty();
    }

    @Test
    @DisplayName("queries는 trim/공백 정규화 후 공백 원소를 제거한다")
    void normalize_whenMultipleSpaces() {
        McpRecommendationQuery query = new McpRecommendationQuery(
                List.of(" SpringBoot  ", "  infra   DevOps ", "   "));

        assertThat(query.queries()).containsExactly("SpringBoot", "infra DevOps");
    }
}
