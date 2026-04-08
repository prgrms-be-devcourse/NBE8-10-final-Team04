package back.domain.comment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import back.domain.comment.dto.CommentInfoResponse;
import back.domain.comment.dto.CreateCommentRequest;
import back.domain.comment.dto.UpdateCommentRequest;
import back.domain.comment.service.CommentService;
import back.global.exception.ServiceException;
import back.global.response.RsData;
import back.global.security.AuthenticatedMember;

class CommentControllerTest {

    private CommentService commentService;
    private CommentController controller;

    @BeforeEach
    void setUp() {
        commentService = mock(CommentService.class);
        controller = new CommentController(commentService);
    }

    @Test
    void getComments_returnsOk() {
        List<CommentInfoResponse> comments = List.of(sampleComment(1L, null, 0));
        when(commentService.getComments(10L)).thenReturn(comments);

        ResponseEntity<RsData<List<CommentInfoResponse>>> response = controller.getComments(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().message()).isEqualTo("댓글 목록 조회 성공");
    }

    @Test
    void createComment_returnsCreated() {
        AuthenticatedMember user = new AuthenticatedMember(2L, "USER");
        CreateCommentRequest request = new CreateCommentRequest(null, "댓글");
        CommentInfoResponse created = sampleComment(1L, null, 0);
        when(commentService.createComment(2L, 10L, request)).thenReturn(created);

        ResponseEntity<RsData<CommentInfoResponse>> response = controller.createComment(user, 10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("댓글 작성 성공");
    }

    @Test
    void updateComment_userRole_passesAdminFalse() {
        AuthenticatedMember user = new AuthenticatedMember(2L, "USER");
        UpdateCommentRequest request = new UpdateCommentRequest("수정");
        CommentInfoResponse updated = sampleComment(1L, null, 0);
        when(commentService.updateComment(2L, false, 10L, 1L, request)).thenReturn(updated);

        ResponseEntity<RsData<CommentInfoResponse>> response = controller.updateComment(user, 10L, 1L, request);

        verify(commentService).updateComment(2L, false, 10L, 1L, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateComment_adminRole_passesAdminTrue() {
        AuthenticatedMember admin = new AuthenticatedMember(1L, "ADMIN");
        UpdateCommentRequest request = new UpdateCommentRequest("수정");
        CommentInfoResponse updated = sampleComment(1L, null, 0);
        when(commentService.updateComment(1L, true, 10L, 1L, request)).thenReturn(updated);

        controller.updateComment(admin, 10L, 1L, request);

        verify(commentService).updateComment(1L, true, 10L, 1L, request);
    }

    @Test
    void deleteComment_passesAdminFlag() {
        AuthenticatedMember admin = new AuthenticatedMember(1L, "ADMIN");

        ResponseEntity<RsData<Void>> response = controller.deleteComment(admin, 10L, 1L);

        verify(commentService).deleteComment(1L, true, 10L, 1L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("댓글 삭제 성공");
    }

    @Test
    void createComment_withoutAuthenticatedMember_throwsUnauthorized() {
        CreateCommentRequest request = new CreateCommentRequest(null, "댓글");

        assertThatThrownBy(() -> controller.createComment(null, 10L, request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("resolveAuthenticatedMemberId");
    }

    private CommentInfoResponse sampleComment(Long id, Long parentId, int depth) {
        return new CommentInfoResponse(
                id,
                10L,
                2L,
                "user",
                parentId,
                depth,
                "내용",
                false,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}

