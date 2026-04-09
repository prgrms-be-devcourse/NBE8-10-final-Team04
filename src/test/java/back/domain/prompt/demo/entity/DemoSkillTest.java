package back.domain.prompt.demo.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.prompt.enums.Category;

class DemoSkillTest {

    @Test
    @DisplayName("DemoSkill builder와 markChunked가 정상 동작한다")
    void demoSkill_builderAndMarkChunked() {
        DemoSkill demoSkill = DemoSkill.builder()
                .skillName("springboot-patterns")
                .repositoryName("demo-repo")
                .repositoryUrl("https://example.com/repo")
                .summary("summary")
                .contentMd("content")
                .category(Category.BACKEND)
                .isChunked(false)
                .forks(3)
                .stars(30)
                .sourceUpdatedAt(OffsetDateTime.parse("2026-04-09T00:00:00Z"))
                .tags(Set.of("spring"))
                .build();

        assertThat(demoSkill.getSkillName()).isEqualTo("springboot-patterns");
        assertThat(demoSkill.isChunked()).isFalse();
        assertThat(demoSkill.getTags()).containsExactly("spring");
        assertThat(demoSkill.getBoostScore()).isEqualTo(0.1f);

        demoSkill.markChunked();
        assertThat(demoSkill.isChunked()).isTrue();
    }
}
