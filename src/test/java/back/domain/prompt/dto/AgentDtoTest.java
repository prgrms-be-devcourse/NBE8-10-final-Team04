package back.domain.prompt.prompt.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentDtoTest {

    @Test
    @DisplayName("rawMetadata accessor는 전달된 맵을 그대로 반환한다")
    void rawMetadataReturnsAssignedValue() {
        Map<String, Object> rawMetadata = new HashMap<>(Map.of("a", 1));
        AgentDto agentDto = new AgentDto("codex", "AGENTS.md", "content", "hash", rawMetadata);

        Map<String, Object> returned = agentDto.rawMetadata();

        assertThat(returned).isSameAs(rawMetadata);
        assertThat(returned).containsEntry("a", 1);
    }

    @Test
    @DisplayName("rawMetadata가 null이면 null을 반환한다")
    void rawMetadataReturnsNullWhenNull() {
        AgentDto agentDto = new AgentDto("codex", "AGENTS.md", "content", "hash", null);

        assertThat(agentDto.rawMetadata()).isNull();
    }
}
