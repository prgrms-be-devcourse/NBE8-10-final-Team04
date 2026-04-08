package back.domain.comment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.comment.entity.Comment;
import back.domain.communitypost.entity.CommunityPost;
import back.domain.communitypost.entity.CommunityPostStatus;
import back.domain.communitypost.entity.CommunityPostType;
import back.domain.member.entity.Member;

class CommentInfoResponseTest {

    @Test
    void from_mapsCommentFields() {
        Member admin = Member.createAdmin("admin-sub", "admin@example.com", "Admin");
        Member user = Member.createUser("user-sub", "user@example.com", "User");
        ReflectionTestUtils.setField(user, "id", 3L);

        CommunityPost post = CommunityPost.createPendingReview(
                CommunityPostType.MODEL_INFO, "제목", "요약", "본문", admin, LocalDate.of(2026, 4, 8), 10L);
        post.changeStatus(CommunityPostStatus.PUBLISHED);
        ReflectionTestUtils.setField(post, "id", 20L);

        Comment root = Comment.createRoot(post, user, "루트 댓글");
        ReflectionTestUtils.setField(root, "id", 30L);

        Comment reply = Comment.createReply(post, user, root, "대댓글");
        ReflectionTestUtils.setField(reply, "id", 31L);

        CommentInfoResponse result = CommentInfoResponse.from(reply);

        assertThat(result.id()).isEqualTo(31L);
        assertThat(result.postId()).isEqualTo(20L);
        assertThat(result.authorId()).isEqualTo(3L);
        assertThat(result.parentId()).isEqualTo(30L);
        assertThat(result.depth()).isEqualTo(1);
        assertThat(result.content()).isEqualTo("대댓글");
    }
}

