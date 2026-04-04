package back.domain.info.dto.response;

import back.domain.info.entity.UpdateRequest;
import back.domain.info.enums.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UpdateRequestResponse(
        Long id,
        String sourceId,
        String vendorName,
        String familyName,
        String sourceUrl,
        String sourceType,
        String summary,
        Status status,
        LocalDate notifiedAt,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
    public UpdateRequestResponse(UpdateRequest entity) {
        this(
                entity.getId(),
                entity.getSourceId(),
                entity.getVendor().getName(),
                entity.getFamily() != null ? entity.getFamily().getFamilyName() : null,
                entity.getSourceUrl(),
                entity.getSourceType(),
                entity.getSummary(),
                entity.getStatus(),
                entity.getNotifiedAt(),
                entity.getReviewedAt(),
                entity.getCreatedAt()
        );
    }
}
