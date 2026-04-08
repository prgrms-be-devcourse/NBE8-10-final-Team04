package back.domain.comment.service;

import java.util.List;

import back.domain.comment.dto.CommentInfoResponse;
import back.domain.comment.dto.CreateCommentRequest;
import back.domain.comment.dto.UpdateCommentRequest;

public interface CommentService {

    List<CommentInfoResponse> getComments(long postId);

    CommentInfoResponse createComment(long memberId, long postId, CreateCommentRequest request);

    CommentInfoResponse updateComment(
            long memberId, boolean admin, long postId, long commentId, UpdateCommentRequest request);

    void deleteComment(long memberId, boolean admin, long postId, long commentId);
}
