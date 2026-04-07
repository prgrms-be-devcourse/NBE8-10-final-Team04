package back.domain.prompt.prompt.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptRepoItemTest {

    @Test
    @DisplayName("skills accessor는 전달된 리스트를 그대로 반환한다")
    void skillsReturnsAssignedList() {
        SkillDto first = new SkillDto("alpha", "skills/alpha.md", "content", "hash", null);
        List<SkillDto> skills = new ArrayList<>(List.of(first));
        PromptRepoItem promptRepoItem = new PromptRepoItem(null, skills, null);

        List<SkillDto> returned = promptRepoItem.skills();

        assertThat(returned).isSameAs(skills);
        assertThat(returned).hasSize(1);
        assertThat(returned.getFirst().name()).isEqualTo("alpha");
    }

    @Test
    @DisplayName("skills가 null이면 null을 반환한다")
    void skillsReturnsNullWhenNull() {
        PromptRepoItem promptRepoItem = new PromptRepoItem(null, null, null);

        assertThat(promptRepoItem.skills()).isNull();
    }
}
