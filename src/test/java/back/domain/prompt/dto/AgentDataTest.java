package back.domain.prompt.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class AgentDataTest {

    @Test
    @DisplayName("rawMetadata getter는 설정된 맵 값을 그대로 반환한다")
    void getRawMetadataReturnsAssignedValue() {
        AgentData agentData = new AgentData();
        Map<String, Object> rawMetadata = new HashMap<>(Map.of("a", 1));
        ReflectionTestUtils.setField(agentData, "rawMetadata", rawMetadata);

        Map<String, Object> returned = agentData.getRawMetadata();

        assertThat(returned).isSameAs(rawMetadata);
        assertThat(returned).containsEntry("a", 1);
    }

    @Test
    @DisplayName("rawMetadata가 null이면 null을 반환한다")
    void getRawMetadataReturnsNullWhenNull() {
        AgentData agentData = new AgentData();

        assertThat(agentData.getRawMetadata()).isNull();
    }
}
