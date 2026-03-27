package back.domain.prompt.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class SkillDataTest {

    @Test
    @DisplayName("rawMetadata getter는 설정된 맵 값을 그대로 반환한다")
    void getRawMetadataReturnsAssignedValue() {
        SkillData skillData = new SkillData();
        Map<String, Object> rawMetadata = new HashMap<>(Map.of("k", "v"));
        ReflectionTestUtils.setField(skillData, "rawMetadata", rawMetadata);

        Map<String, Object> returned = skillData.getRawMetadata();

        assertThat(returned).isSameAs(rawMetadata);
        assertThat(returned).containsEntry("k", "v");
    }

    @Test
    @DisplayName("rawMetadata가 null이면 null을 반환한다")
    void getRawMetadataReturnsNullWhenNull() {
        SkillData skillData = new SkillData();

        assertThat(skillData.getRawMetadata()).isNull();
    }
}
