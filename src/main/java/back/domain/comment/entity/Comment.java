package back.domain.comment.entity;

import java.time.LocalDateTime;

import back.domain.communitypost.entity.CommunityPost;
import back.domain.member.entity.Member;
import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_community_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "CT_CONSTRUCTOR_THROW"},
        justification = "JPA 엔티티 참조 노출은 의도된 패턴이며, 생성자 검증 예외는 도메인 무결성 보장을 위한 것이다.")
public class Comment extends BaseEntity {

    private static final String DELETED_CONTENT = "삭제된 댓글입니다.";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id", nullable = false)
    private Member author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(nullable = false)
    private int depth;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Comment(CommunityPost post, Member author, Comment parent, int depth, String content) {
        this.post = post;
        this.author = author;
        this.parent = parent;
        this.depth = depth;
        this.content = requireNotBlank(content, "content");
        this.deleted = false;
    }

    public static Comment createRoot(CommunityPost post, Member author, String content) {
        return new Comment(post, author, null, 0, content);
    }

    public static Comment createReply(CommunityPost post, Member author, Comment parent, String content) {
        return new Comment(post, author, parent, 1, content);
    }

    public void updateContent(String content) {
        if (deleted) {
            throw new IllegalStateException("deleted comment can not be updated");
        }
        this.content = requireNotBlank(content, "content");
    }

    public void softDelete() {
        if (deleted) {
            return;
        }

        this.content = DELETED_CONTENT;
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    private String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
