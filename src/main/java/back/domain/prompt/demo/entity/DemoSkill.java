package back.domain.prompt.demo.entity;

import back.domain.prompt.prompt.enums.Category;
import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "demo_skills")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "JPA 엔티티의 컬렉션 필드와 빌더 인자는 영속성 컨텍스트에서 관리됩니다."
)
public class DemoSkill extends BaseEntity {

    /**
     * JSON 원본의 skill_id.
     * 외부 mock 데이터 식별용.
     */
    @Column(name = "skill_name", nullable = false, length = 255)
    private String skillName;

    @Column(name = "repository_name", nullable = false, length = 255)
    private String repositoryName;

    @Column(name = "repository_url", nullable = false, length = 1000)
    private String repositoryUrl;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private Category category;

    @Column(name = "is_chunked", nullable = false)
    private boolean isChunked;

    @Column(name = "forks", nullable = false)
    private int forks;

    @Column(name = "stars", nullable = false)
    private int stars;

    @Column(name = "source_updated_at")
    private OffsetDateTime sourceUpdatedAt;

    // 검색 결과 순위 조작용 가중치 — 값이 클수록 상위 노출
    @Builder.Default
    @Column(name = "boost_score", nullable = false)
    private float boostScore = 0.1f;

    @Column(name = "tag", nullable = false, length = 100)
    @Builder.Default
    private Set<String> tags = new HashSet<>();


    public void markChunked() {
        this.isChunked = true;
    }

}
