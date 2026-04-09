package back.domain.prompt.prompt.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SkillListItemDtoTest {

    @Test
    @DisplayName("생성자는 null tags를 빈 Set으로 치환한다")
    void constructor_replacesNullTagsWithEmptySet() {
        SkillListItemDto dto = new SkillListItemDto(
                1L, "alpha", "BACKEND", null,
                "repo", "https://example.com/repo", "summary", 10, 2);

        assertThat(dto.tags()).isEmpty();
        assertThatThrownBy(() -> dto.tags().add("new-tag"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("생성자는 tags를 방어 복사한다")
    void constructor_defensivelyCopiesTags() {
        Set<String> originalTags = new HashSet<>(Set.of("java"));

        SkillListItemDto dto = new SkillListItemDto(
                1L, "alpha", "BACKEND", originalTags,
                "repo", "https://example.com/repo", "summary", 10, 2);
        originalTags.add("spring");

        assertThat(dto.tags()).containsExactly("java");
    }

    @Test
    @DisplayName("from은 Skill과 Repository 필드를 SkillListItemDto로 매핑한다")
    void from_mapsSkillAndRepositoryFields() {
        Skill skill = skill(8L, "recommend", Category.AUTOMATION, "body", Set.of("agent", "workflow"));

        SkillListItemDto dto = SkillListItemDto.from(skill);

        assertThat(dto.id()).isEqualTo(8L);
        assertThat(dto.name()).isEqualTo("recommend");
        assertThat(dto.category()).isEqualTo("AUTOMATION");
        assertThat(dto.tags()).containsExactlyInAnyOrder("agent", "workflow");
        assertThat(dto.repositoryName()).isEqualTo("demo-repo");
        assertThat(dto.repositoryUrl()).isEqualTo("https://example.com/owner/repo");
        assertThat(dto.summary()).isEqualTo("demo summary");
        assertThat(dto.stars()).isEqualTo(10);
        assertThat(dto.forks()).isEqualTo(3);
    }

    private Skill skill(Long id, String name, Category category, String contentMd, Set<String> tags) {
        Skill skill = Skill.builder()
                .repository(repository())
                .name(name)
                .contentMd(contentMd)
                .contentHash(name + "-hash")
                .filePath("skills/" + name + ".md")
                .category(category)
                .tagsJson(tags)
                .build();
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }

    private Repository repository() {
        Repository repository = Repository.builder()
                .githubId(100L)
                .name("demo-repo")
                .sourceRepo("owner/repo")
                .sourceUri("https://example.com/owner/repo")
                .summary("demo summary")
                .starCount(10)
                .forkCount(3)
                .size(50)
                .languageStats(Map.of("Java", 90))
                .license("MIT")
                .homepage("https://example.com")
                .ownerAvatarUrl("https://example.com/avatar.png")
                .ownerType(OwnerType.USER)
                .isOfficial(true)
                .defaultBranch("main")
                .etag("etag-1")
                .sourceUpdatedAt(LocalDateTime.parse("2026-03-26T00:00:00"))
                .active(true)
                .rawMetadata(Map.of("category", "demo"))
                .build();
        ReflectionTestUtils.setField(repository, "id", 1000L);
        return repository;
    }
}
