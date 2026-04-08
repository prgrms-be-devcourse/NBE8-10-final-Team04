package back.domain.prompt.search.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.prompt.search.dto.chunk.SkillChunkVectorSearchRowDto;
import back.domain.prompt.search.dto.search.QuerySearchHit;
import back.domain.prompt.search.dto.search.SearchQueryDto;
import back.domain.prompt.search.enums.QueryType;

class QuerySearchDtoTest {

    @Test
    @DisplayName("SearchQueryDto는 text와 type을 보관한다")
    void searchQueryDto_storesFields() {
        SearchQueryDto dto = new SearchQueryDto("spring boot", QueryType.TECH);

        assertThat(dto.text()).isEqualTo("spring boot");
        assertThat(dto.type()).isEqualTo(QueryType.TECH);
    }

    @Test
    @DisplayName("QuerySearchHit은 query, queryType, row를 보관한다")
    void querySearchHit_storesFields() {
        SkillChunkVectorSearchRowDto row = new SkillChunkVectorSearchRowDto(
                1L,
                2L,
                "skill-name",
                "repo-name",
                "https://example.com/repo",
                "BACKEND",
                "summary",
                10,
                3,
                LocalDateTime.parse("2026-04-01T00:00:00"),
                0.91f
        );

        QuerySearchHit hit = new QuerySearchHit("spring search", QueryType.FUNCTION, row);

        assertThat(hit.query()).isEqualTo("spring search");
        assertThat(hit.queryType()).isEqualTo(QueryType.FUNCTION);
        assertThat(hit.row()).isSameAs(row);
    }
}
