package back.domain.communitypost.dto;

import back.domain.communitypost.entity.CommunityPostStatus;
import jakarta.validation.constraints.NotNull;

public record AdminChangeCommunityPostStatusRequest(
        @NotNull(message = "status-NotNull-게시글 상태는 필수입니다.") CommunityPostStatus status) {}
