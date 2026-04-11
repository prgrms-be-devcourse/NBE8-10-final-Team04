package back.domain.prompt.demo.repository;

import back.domain.prompt.demo.dto.DemoChunkVectorSearchRowDto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "공유 JdbcTemplate 빈은 스프링이 주입하고 관리한다.")
public class DemoSkillChunkVectorSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    // 데모 청크 전체를 벡터 유사도 순으로 반환한다.
    // 데모 스킬이 소수이므로 LIMIT 없이 항상 모든 후보를 반환한다.
    public List<DemoChunkVectorSearchRowDto> searchAll(String queryVector) {
        String sql = """
                SELECT
                    dc.id          AS chunk_id,
                    ds.id          AS skill_id,
                    ds.skill_name,
                    ds.repository_name,
                    ds.repository_url,
                    ds.category,
                    ds.summary,
                    ds.stars,
                    ds.forks,
                    ds.source_updated_at AS updated_at,
                    1 - (dc.embedding <=> CAST(? AS vector)) AS similarity,
                    ds.boost_score
                FROM demo_skill_chunks dc
                JOIN demo_skills ds ON ds.id = dc.demo_skill_id
                WHERE dc.embedding IS NOT NULL
                ORDER BY dc.embedding <=> CAST(? AS vector)
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DemoChunkVectorSearchRowDto(
                        rs.getLong("chunk_id"),
                        rs.getLong("skill_id"),
                        rs.getString("skill_name"),
                        rs.getString("repository_name"),
                        rs.getString("repository_url"),
                        rs.getString("category"),
                        rs.getString("summary"),
                        rs.getObject("stars", Integer.class),
                        rs.getObject("forks", Integer.class),
                        rs.getObject("updated_at", OffsetDateTime.class),
                        rs.getFloat("similarity"),
                        rs.getFloat("boost_score")
                ),
                queryVector,
                queryVector
        );
    }
}
