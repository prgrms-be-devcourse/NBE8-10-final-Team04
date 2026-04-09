package back.domain.communitypost.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import back.domain.communitypost.entity.CommunityPost;
import back.domain.communitypost.entity.CommunityPostStatus;
import back.domain.communitypost.entity.CommunityPostType;

public record CommunityPostInfoResponse(
        Long id,
        CommunityPostType type,
        String title,
        String summary,
        String sourceUrl,
        CommunityPostStatus status,
        LocalDate targetDate,
        Long vendorId,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static CommunityPostInfoResponse from(CommunityPost post) {
        return new CommunityPostInfoResponse(
                post.getId(),
                post.getPostType(),
                post.getTitle(),
                post.getSummary(),
                post.getSourceUrl(),
                post.getStatus(),
                post.getTargetDate(),
                post.getPostType() == CommunityPostType.MODEL_INFO ? post.getVendorId() : null,
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
