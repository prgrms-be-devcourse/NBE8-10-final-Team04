package back.domain.comment.dto;

import java.time.LocalDateTime;

import back.domain.comment.entity.Comment;

public record CommentInfoResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        Long parentId,
        int depth,
        String content,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static CommentInfoResponse from(Comment comment) {
        return new CommentInfoResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getDepth(),
                comment.getContent(),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
