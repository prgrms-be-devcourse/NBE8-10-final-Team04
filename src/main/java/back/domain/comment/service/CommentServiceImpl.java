package back.domain.comment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import back.domain.comment.dto.CommentInfoResponse;
import back.domain.comment.dto.CreateCommentRequest;
import back.domain.comment.dto.UpdateCommentRequest;
import back.domain.comment.entity.Comment;
import back.domain.comment.repository.CommentRepository;
import back.domain.communitypost.entity.CommunityPost;
import back.domain.communitypost.service.CommunityPostReadService;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "스프링 DI로 주입되는 빈 참조이며 의도된 패턴이다.")
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final int MAX_DEPTH = 1;

    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final CommunityPostReadService communityPostReadService;

    @Override
    public List<CommentInfoResponse> getComments(long postId) {
        communityPostReadService.getPublishedPostOrThrow(postId);
        return commentRepository.findAllByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommentInfoResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public CommentInfoResponse createComment(long memberId, long postId, CreateCommentRequest request) {
        Member author = getMemberOrThrow(memberId);
        CommunityPost post = communityPostReadService.getPublishedPostOrThrow(postId);

        Comment comment;
        if (request.parentCommentId() == null) {
            comment = Comment.createRoot(post, author, request.content());
        } else {
            Comment parent = getCommentOrThrow(postId, request.parentCommentId());
            validateReplyDepth(parent);
            comment = Comment.createReply(post, author, parent, request.content());
        }

        return CommentInfoResponse.from(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentInfoResponse updateComment(
            long memberId, boolean admin, long postId, long commentId, UpdateCommentRequest request) {
        communityPostReadService.getPublishedPostOrThrow(postId);
        Comment comment = getCommentOrThrow(postId, commentId);
        validateCommentOwner(comment, memberId, admin);

        if (comment.isDeleted()) {
            throw new ServiceException(
                    CommonErrorCode.BAD_REQUEST_STATE,
                    "[CommentServiceImpl#updateComment] deleted comment can not be updated",
                    "삭제된 댓글은 수정할 수 없습니다.");
        }

        comment.updateContent(request.content());
        return CommentInfoResponse.from(comment);
    }

    @Override
    @Transactional
    public void deleteComment(long memberId, boolean admin, long postId, long commentId) {
        communityPostReadService.getPublishedPostOrThrow(postId);
        Comment comment = getCommentOrThrow(postId, commentId);
        validateCommentOwner(comment, memberId, admin);
        comment.softDelete();
    }

    private void validateReplyDepth(Comment parent) {
        if (parent.getDepth() >= MAX_DEPTH) {
            throw new ServiceException(
                    CommonErrorCode.BAD_REQUEST,
                    "[CommentServiceImpl#validateReplyDepth] only depth 1 reply is allowed",
                    "대댓글까지만 작성할 수 있습니다.");
        }
    }

    private void validateCommentOwner(Comment comment, long memberId, boolean admin) {
        if (admin) {
            return;
        }

        if (!comment.getAuthor().getId().equals(memberId)) {
            throw new ServiceException(
                    CommonErrorCode.FORBIDDEN,
                    "[CommentServiceImpl#validateCommentOwner] requester is not comment owner",
                    "댓글 작성자만 수정 또는 삭제할 수 있습니다.");
        }
    }

    private Comment getCommentOrThrow(long postId, long commentId) {
        return commentRepository
                .findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new ServiceException(
                        CommonErrorCode.NOT_FOUND,
                        "[CommentServiceImpl#getCommentOrThrow] comment not found",
                        "댓글을 찾을 수 없습니다."));
    }

    private Member getMemberOrThrow(long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ServiceException(
                        CommonErrorCode.NOT_FOUND,
                        "[CommentServiceImpl#getMemberOrThrow] member not found",
                        "회원이 존재하지 않습니다."));
    }
}
