package back.domain.prompt.embedding.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;

class SkillChunkTest {

    @Test
    @DisplayName("builder와 updateEmbedding은 SkillChunk 값을 갱신한다")
    void builderAndUpdateEmbedding_updateFields() {
        SkillChunk skillChunk = SkillChunk.builder()
                .skill(skill())
                .chunkIndex(1)
                .sectionTitle("Install")
                .sectionPath("Install > Basic")
                .searchText("install basic")
                .charCount(123)
                .chunkVersion("v1")
                .embeddingModel("old-model")
                .embedding(new float[]{0.1f})
                .embeddedAt(OffsetDateTime.parse("2026-03-31T00:00:00Z"))
                .build();

        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-04-01T00:00:00Z");
        skillChunk.updateEmbedding(new float[]{0.3f, 0.4f}, "new-model", updatedAt);

        assertThat(skillChunk.getChunkIndex()).isEqualTo(1);
        assertThat(skillChunk.getSectionTitle()).isEqualTo("Install");
        assertThat(skillChunk.getEmbedding()).containsExactly(0.3f, 0.4f);
        assertThat(skillChunk.getEmbeddingModel()).isEqualTo("new-model");
        assertThat(skillChunk.getEmbeddedAt()).isEqualTo(updatedAt);
    }

    private Skill skill() {
        return Skill.builder()
                .repository(Repository.builder()
                        .githubId(100L)
                        .name("demo-repo")
                        .sourceRepo("owner/repo")
                        .sourceUri("https://example.com/owner/repo")
                        .summary("demo summary")
                        .starCount(10)
                        .forkCount(2)
                        .size(100)
                        .languageStats(Map.of("Java", 100))
                        .license("MIT")
                        .homepage("https://example.com")
                        .ownerAvatarUrl("https://example.com/avatar.png")
                        .ownerType(OwnerType.USER)
                        .isOfficial(true)
                        .defaultBranch("main")
                        .etag("etag")
                        .sourceUpdatedAt(LocalDateTime.parse("2026-03-31T00:00:00"))
                        .active(true)
                        .build())
                .name("alpha")
                .contentMd("alpha content")
                .contentHash("alpha-hash")
                .filePath("skills/alpha.md")
                .category(Category.BACKEND)
                .tagsJson(Set.of("spring"))
                .build();
    }
}
