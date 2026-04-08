package back.domain.communitypost.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import back.domain.member.entity.Member;
import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_community_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "CT_CONSTRUCTOR_THROW"},
        justification = "JPA 엔티티 참조 노출은 의도된 패턴이며, 생성자 검증 예외는 도메인 무결성 보장을 위한 것이다.")
public class CommunityPost extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 50)
    private CommunityPostType postType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CommunityPostStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_admin_id", nullable = false)
    private Member authorAdmin;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private CommunityPost(
            CommunityPostType postType,
            String title,
            String summary,
            String body,
            CommunityPostStatus status,
            Member authorAdmin,
            LocalDate targetDate,
            Long vendorId) {
        this.postType = postType;
        this.title = requireNotBlank(title, "title");
        this.summary = summary == null ? null : summary.trim();
        this.body = requireNotBlank(body, "body");
        this.status = status;
        this.authorAdmin = authorAdmin;
        this.targetDate = targetDate;
        this.vendorId = vendorId;
    }

    public static CommunityPost createPendingReview(
            CommunityPostType postType,
            String title,
            String summary,
            String body,
            Member authorAdmin,
            LocalDate targetDate,
            Long vendorId) {
        return new CommunityPost(
                postType, title, summary, body, CommunityPostStatus.PENDING_REVIEW, authorAdmin, targetDate, vendorId);
    }

    public void updateContent(String title, String summary, String body) {
        this.title = requireNotBlank(title, "title");
        this.summary = summary == null ? null : summary.trim();
        this.body = requireNotBlank(body, "body");
    }

    public void changeStatus(CommunityPostStatus status) {
        this.status = status;
        if (status == CommunityPostStatus.PUBLISHED) {
            if (publishedAt == null) {
                publishedAt = LocalDateTime.now();
            }
            return;
        }

        this.publishedAt = null;
    }

    private String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
