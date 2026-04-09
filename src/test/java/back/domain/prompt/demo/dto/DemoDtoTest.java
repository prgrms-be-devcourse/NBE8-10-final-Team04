package back.domain.prompt.demo.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.search.enums.QueryType;

class DemoDtoTest {

    @Test
    @DisplayName("DemoSkillRequestDto 필드를 보관한다")
    void demoSkillRequestDto_storesFields() {
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-04-09T00:00:00Z");
        DemoSkillRequestDto dto = new DemoSkillRequestDto(
                1L,
                "springboot-patterns",
                "demo-repo",
                "https://example.com/repo",
                "summary",
                "content",
                Category.BACKEND,
                false,
                2,
                10,
                updatedAt,
                List.of("spring"),
                List.of("springboot")
        );

        assertThat(dto.skillId()).isEqualTo(1L);
        assertThat(dto.skillName()).isEqualTo("springboot-patterns");
        assertThat(dto.updatedAt()).isEqualTo(updatedAt);
        assertThat(dto.aliases()).containsExactly("springboot");
    }

    @Test
    @DisplayName("DemoSkillBatchRequestDto 필드를 보관한다")
    void demoSkillBatchRequestDto_storesFields() {
        DemoSkillRequestDto item = new DemoSkillRequestDto(
                1L, "skill", "repo", "url", "summary", "content",
                Category.BACKEND, false, 0, 0, null, List.of(), List.of()
        );
        DemoSkillBatchRequestDto dto = new DemoSkillBatchRequestDto(List.of(item));

        assertThat(dto.skills()).hasSize(1);
        assertThat(dto.skills().getFirst().skillName()).isEqualTo("skill");
    }

    @Test
    @DisplayName("DemoChunkVectorSearchRowDto와 DemoQuerySearchHit 필드를 보관한다")
    void demoSearchDtos_storeFields() {
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-04-09T00:00:00Z");
        DemoChunkVectorSearchRowDto row = new DemoChunkVectorSearchRowDto(
                11L,
                22L,
                "skill-name",
                "repo-name",
                "https://example.com/repo",
                "BACKEND",
                "summary",
                100,
                10,
                updatedAt,
                0.91f
        );
        DemoQuerySearchHit hit = new DemoQuerySearchHit("spring boot", QueryType.TECH, row);

        assertThat(row.demoSkillId()).isEqualTo(22L);
        assertThat(row.similarity()).isEqualTo(0.91f);
        assertThat(hit.query()).isEqualTo("spring boot");
        assertThat(hit.queryType()).isEqualTo(QueryType.TECH);
        assertThat(hit.row()).isSameAs(row);
    }
}
