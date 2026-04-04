package back.domain.prompt.prompt.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillDtoTest {

    @Test
    @DisplayName("rawMetadata accessor는 전달된 맵을 그대로 반환한다")
    void rawMetadataReturnsAssignedValue() {
        Map<String, Object> rawMetadata = new HashMap<>(Map.of("k", "v"));
        SkillDto skillDto = new SkillDto("alpha", "skills/alpha.md", "content", "hash", rawMetadata);

        Map<String, Object> returned = skillDto.rawMetadata();

        assertThat(returned).isSameAs(rawMetadata);
        assertThat(returned).containsEntry("k", "v");
    }

    @Test
    @DisplayName("rawMetadata가 null이면 null을 반환한다")
    void rawMetadataReturnsNullWhenNull() {
        SkillDto skillDto = new SkillDto("alpha", "skills/alpha.md", "content", "hash", null);

        assertThat(skillDto.rawMetadata()).isNull();
    }
}
