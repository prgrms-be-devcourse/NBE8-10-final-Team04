package back.domain.prompt.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import back.domain.prompt.demo.dto.DemoChunkVectorSearchRowDto;

@ExtendWith(MockitoExtension.class)
class DemoSkillChunkVectorSearchRepositoryTest {

    @Test
    @DisplayName("searchAll은 JdbcTemplate 결과를 Demo DTO로 매핑한다")
    @SuppressWarnings("unchecked")
    void searchAll_mapsJdbcRows() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DemoSkillChunkVectorSearchRepository repository = new DemoSkillChunkVectorSearchRepository(jdbcTemplate);

        doAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            RowMapper<DemoChunkVectorSearchRowDto> rowMapper = invocation.getArgument(1);
            String firstVector = invocation.getArgument(2, String.class);
            String secondVector = invocation.getArgument(3, String.class);

            assertThat(sql).contains("CAST(? AS vector)");
            assertThat(firstVector).isEqualTo("[0.1,0.2]");
            assertThat(secondVector).isEqualTo("[0.1,0.2]");

            ResultSet rs = mock(ResultSet.class);
            org.mockito.Mockito.when(rs.getLong("chunk_id")).thenReturn(1L);
            org.mockito.Mockito.when(rs.getLong("skill_id")).thenReturn(2L);
            org.mockito.Mockito.when(rs.getString("skill_name")).thenReturn("alpha");
            org.mockito.Mockito.when(rs.getString("repository_name")).thenReturn("demo-repo");
            org.mockito.Mockito.when(rs.getString("repository_url")).thenReturn("https://example.com/repo");
            org.mockito.Mockito.when(rs.getString("category")).thenReturn("BACKEND");
            org.mockito.Mockito.when(rs.getString("summary")).thenReturn("summary");
            org.mockito.Mockito.when(rs.getObject("stars", Integer.class)).thenReturn(10);
            org.mockito.Mockito.when(rs.getObject("forks", Integer.class)).thenReturn(2);
            org.mockito.Mockito.when(rs.getObject("updated_at", OffsetDateTime.class))
                    .thenReturn(OffsetDateTime.parse("2026-04-09T00:00:00Z"));
            org.mockito.Mockito.when(rs.getFloat("similarity")).thenReturn(0.92f);
            org.mockito.Mockito.when(rs.getFloat("boost_score")).thenReturn(1.1f);

            return List.of(rowMapper.mapRow(rs, 0));
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), anyString(), anyString());

        List<DemoChunkVectorSearchRowDto> result = repository.searchAll("[0.1,0.2]");

        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row.chunkId()).isEqualTo(1L);
            assertThat(row.demoSkillId()).isEqualTo(2L);
            assertThat(row.skillName()).isEqualTo("alpha");
            assertThat(row.similarity()).isEqualTo(0.92f);
            assertThat(row.boostScore()).isEqualTo(1.1f);
        });
    }
}
