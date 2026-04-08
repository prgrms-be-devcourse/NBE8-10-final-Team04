package back.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(@NotBlank(message = "content-NotBlank-댓글 내용은 필수입니다.") String content) {}
