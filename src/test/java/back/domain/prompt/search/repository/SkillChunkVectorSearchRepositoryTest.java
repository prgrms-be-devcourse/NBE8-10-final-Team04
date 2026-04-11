package back.domain.prompt.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import back.domain.prompt.search.dto.chunk.SkillChunkVectorSearchRowDto;

@ExtendWith(MockitoExtension.class)
class SkillChunkVectorSearchRepositoryTest {

    @Test
    @DisplayName("searchTopK는 JdbcTemplate 결과를 DTO로 매핑한다")
    @SuppressWarnings("unchecked")
    void searchTopK_mapsJdbcRows() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SkillChunkVectorSearchRepository repository = new SkillChunkVectorSearchRepository(jdbcTemplate);

        doAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            PreparedStatementSetter setter = invocation.getArgument(1, PreparedStatementSetter.class);
            RowMapper<SkillChunkVectorSearchRowDto> rowMapper = invocation.getArgument(2);

            assertThat(sql).contains("CAST(? AS vector)");

            PreparedStatement ps = mock(PreparedStatement.class);
            setter.setValues(ps);
            verify(ps).setQueryTimeout(5);
            verify(ps).setString(1, "[0.1,0.2]");
            verify(ps).setString(2, "[0.1,0.2]");
            verify(ps).setInt(3, 5);

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
            org.mockito.Mockito.when(rs.getObject("updated_at", LocalDateTime.class))
                    .thenReturn(LocalDateTime.parse("2026-03-31T00:00:00"));
            org.mockito.Mockito.when(rs.getFloat("similarity")).thenReturn(0.91f);

            return List.of(rowMapper.mapRow(rs, 0));
        }).when(jdbcTemplate).query(anyString(), org.mockito.ArgumentMatchers.any(PreparedStatementSetter.class), org.mockito.ArgumentMatchers.any(RowMapper.class));

        List<SkillChunkVectorSearchRowDto> result = repository.searchTopK("[0.1,0.2]", 5);

        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row.chunkId()).isEqualTo(1L);
            assertThat(row.skillId()).isEqualTo(2L);
            assertThat(row.skillName()).isEqualTo("alpha");
            assertThat(row.similarity()).isEqualTo(0.91f);
        });
    }
}
