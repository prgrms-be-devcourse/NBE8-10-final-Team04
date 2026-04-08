package back.domain.comment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import back.domain.comment.dto.CommentInfoResponse;
import back.domain.comment.dto.CreateCommentRequest;
import back.domain.comment.dto.UpdateCommentRequest;
import back.domain.comment.controller.docs.CommentControllerDocs;
import back.domain.comment.service.CommentService;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.response.RsData;
import back.global.security.AuthenticatedMember;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/community/posts/{postId}/comments")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "스프링 DI로 주입되는 빈 참조이며 의도된 패턴이다.")
public class CommentController implements CommentControllerDocs {

    private static final String ADMIN_ROLE = "ADMIN";

    private final CommentService commentService;

    @Override
    @GetMapping
    public ResponseEntity<RsData<List<CommentInfoResponse>>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(new RsData<>(commentService.getComments(postId), "댓글 목록 조회 성공"));
    }

    @Override
    @PostMapping
    public ResponseEntity<RsData<CommentInfoResponse>> createComment(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        long memberId = resolveAuthenticatedMemberId(authenticatedMember);
        CommentInfoResponse response = commentService.createComment(memberId, postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RsData<>(response, "댓글 작성 성공"));
    }

    @Override
    @PutMapping("/{commentId}")
    public ResponseEntity<RsData<CommentInfoResponse>> updateComment(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        long memberId = resolveAuthenticatedMemberId(authenticatedMember);
        boolean admin = isAdmin(authenticatedMember);
        CommentInfoResponse response = commentService.updateComment(memberId, admin, postId, commentId, request);
        return ResponseEntity.ok(new RsData<>(response, "댓글 수정 성공"));
    }

    @Override
    @DeleteMapping("/{commentId}")
    public ResponseEntity<RsData<Void>> deleteComment(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        long memberId = resolveAuthenticatedMemberId(authenticatedMember);
        boolean admin = isAdmin(authenticatedMember);
        commentService.deleteComment(memberId, admin, postId, commentId);
        return ResponseEntity.ok(new RsData<>("댓글 삭제 성공"));
    }

    private long resolveAuthenticatedMemberId(AuthenticatedMember authenticatedMember) {
        if (authenticatedMember == null) {
            throw new ServiceException(
                    CommonErrorCode.UNAUTHORIZED,
                    "[CommentController#resolveAuthenticatedMemberId] authenticated member is missing",
                    CommonErrorCode.UNAUTHORIZED.defaultMessage());
        }

        return authenticatedMember.memberId();
    }

    private boolean isAdmin(AuthenticatedMember authenticatedMember) {
        return authenticatedMember != null && ADMIN_ROLE.equals(authenticatedMember.role());
    }
}
