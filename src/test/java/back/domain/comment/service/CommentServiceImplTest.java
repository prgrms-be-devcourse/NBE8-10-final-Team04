package back.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.comment.dto.CreateCommentRequest;
import back.domain.comment.dto.UpdateCommentRequest;
import back.domain.comment.entity.Comment;
import back.domain.comment.repository.CommentRepository;
import back.domain.communitypost.entity.CommunityPost;
import back.domain.communitypost.entity.CommunityPostType;
import back.domain.communitypost.service.CommunityPostReadService;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CommunityPostReadService communityPostReadService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(commentRepository, memberRepository, communityPostReadService);
    }

    @Test
    @DisplayName("대댓글의 대댓글 작성은 거부한다(depth 1까지만 허용)")
    void createComment_whenParentDepthIsOne_thenThrowBadRequest() {
        long postId = 10L;
        long memberId = 3L;
        long parentId = 44L;

        CommunityPost publishedPost = CommunityPost.createPendingReview(
                CommunityPostType.MODEL_INFO,
                "title",
                "summary",
                "body",
                Member.createAdmin("admin-sub", "admin@example.com", "Admin"),
                java.time.LocalDate.now(),
                null);
        publishedPost.changeStatus(back.domain.communitypost.entity.CommunityPostStatus.PUBLISHED);
        ReflectionTestUtils.setField(publishedPost, "id", postId);

        Member author = Member.createUser("sub-1", "user1@example.com", "User 1");

        Comment root = Comment.createRoot(publishedPost, author, "root");
        ReflectionTestUtils.setField(root, "id", 41L);

        Comment depthOne = Comment.createReply(publishedPost, author, root, "reply");
        ReflectionTestUtils.setField(depthOne, "id", parentId);

        when(memberRepository.findById(memberId)).thenReturn(java.util.Optional.of(author));
        when(communityPostReadService.getPublishedPostOrThrow(postId)).thenReturn(publishedPost);
        when(commentRepository.findByIdAndPostId(parentId, postId)).thenReturn(java.util.Optional.of(depthOne));

        assertThatThrownBy(() ->
                        commentService.createComment(memberId, postId, new CreateCommentRequest(parentId, "depth2")))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("validateReplyDepth");
    }

    @Test
    @DisplayName("관리자는 타인 댓글도 소프트 삭제할 수 있다")
    void deleteComment_byAdmin_softDelete() {
        long postId = 10L;
        long commentId = 45L;

        CommunityPost publishedPost = CommunityPost.createPendingReview(
                CommunityPostType.MODEL_INFO,
                "title",
                "summary",
                "body",
                Member.createAdmin("admin-sub", "admin@example.com", "Admin"),
                java.time.LocalDate.now(),
                null);
        publishedPost.changeStatus(back.domain.communitypost.entity.CommunityPostStatus.PUBLISHED);
        ReflectionTestUtils.setField(publishedPost, "id", postId);

        Member author = Member.createUser("sub-2", "user2@example.com", "User 2");
        Comment rootComment = Comment.createRoot(publishedPost, author, "original content");
        ReflectionTestUtils.setField(rootComment, "id", commentId);

        when(communityPostReadService.getPublishedPostOrThrow(postId)).thenReturn(publishedPost);
        when(commentRepository.findByIdAndPostId(commentId, postId)).thenReturn(java.util.Optional.of(rootComment));

        commentService.deleteComment(999L, true, postId, commentId);

        assertThat(rootComment.isDeleted()).isTrue();
        assertThat(rootComment.getContent()).isEqualTo("삭제된 댓글입니다.");
    }

    @Test
    @DisplayName("삭제된 댓글은 수정할 수 없다")
    void updateComment_whenDeleted_thenThrowBadRequestState() {
        long postId = 10L;
        long commentId = 46L;

        CommunityPost publishedPost = CommunityPost.createPendingReview(
                CommunityPostType.MODEL_INFO,
                "title",
                "summary",
                "body",
                Member.createAdmin("admin-sub", "admin@example.com", "Admin"),
                java.time.LocalDate.now(),
                null);
        publishedPost.changeStatus(back.domain.communitypost.entity.CommunityPostStatus.PUBLISHED);
        ReflectionTestUtils.setField(publishedPost, "id", postId);

        Member author = Member.createUser("sub-3", "user3@example.com", "User 3");
        Comment rootComment = Comment.createRoot(publishedPost, author, "before delete");
        ReflectionTestUtils.setField(rootComment, "id", commentId);
        rootComment.softDelete();

        when(communityPostReadService.getPublishedPostOrThrow(postId)).thenReturn(publishedPost);
        when(commentRepository.findByIdAndPostId(commentId, postId)).thenReturn(java.util.Optional.of(rootComment));

        assertThatThrownBy(() -> commentService.updateComment(
                        1L, true, postId, commentId, new UpdateCommentRequest("new content")))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("updateComment");
    }
}
