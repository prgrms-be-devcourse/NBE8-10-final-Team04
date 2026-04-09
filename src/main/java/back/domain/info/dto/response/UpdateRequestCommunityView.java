package back.domain.info.dto.response;

import java.time.LocalDate;

import back.domain.info.entity.UpdateRequest;

public record UpdateRequestCommunityView(
        String vendorName,
        String familyName,
        String sourceUrl,
        String sourceType,
        String summary,
        String rawContent,
        LocalDate notifiedAt) {

    public static UpdateRequestCommunityView from(UpdateRequest updateRequest) {
        return new UpdateRequestCommunityView(
                updateRequest.getVendor().getName(),
                updateRequest.getFamily() == null ? null : updateRequest.getFamily().getFamilyName(),
                updateRequest.getSourceUrl(),
                updateRequest.getSourceType(),
                updateRequest.getSummary(),
                updateRequest.getRawContent(),
                updateRequest.getNotifiedAt());
    }
}
