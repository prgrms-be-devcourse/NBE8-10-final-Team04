package back.domain.prompt.demo.entity;

import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "demo_skill_chunks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "JPA 엔티티의 가변 참조는 영속성 컨텍스트에서 관리한다.")
public class DemoSkillChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demo_skill_id", nullable = false)
    private DemoSkill demoSkill;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "section_title")
    private String sectionTitle;

    @Column(name = "search_text", nullable = false, columnDefinition = "TEXT")
    private String searchText;

    @Column(name = "char_count")
    private Integer charCount;

    @Column(name = "chunk_version", length = 50)
    private String chunkVersion;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private float[] embedding;

    @Column(name = "embedded_at")
    private OffsetDateTime embeddedAt;
}
