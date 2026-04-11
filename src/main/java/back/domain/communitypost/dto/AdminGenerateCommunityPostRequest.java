package back.domain.communitypost.dto;

import java.time.LocalDate;

import back.domain.communitypost.entity.CommunityPostType;
import jakarta.validation.constraints.NotNull;

public record AdminGenerateCommunityPostRequest(
        @NotNull(message = "type-NotNull-게시글 타입은 필수입니다.") CommunityPostType type,
        @NotNull(message = "targetDate-NotNull-기준 날짜는 필수입니다.") LocalDate targetDate,
        Long vendorId) {}
