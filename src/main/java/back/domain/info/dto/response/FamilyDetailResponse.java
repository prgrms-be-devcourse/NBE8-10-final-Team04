package back.domain.info.dto.response;

import java.time.LocalDateTime;

import back.domain.info.entity.AiModelFamily;

public record FamilyDetailResponse(
        Long id,
        String vendorName,
        String familyName,
        String commonDescription,
        String[] inputTypes,
        String[] outputTypes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public FamilyDetailResponse {
        inputTypes = inputTypes == null ? null : inputTypes.clone();
        outputTypes = outputTypes == null ? null : outputTypes.clone();
    }

    public static FamilyDetailResponse from(AiModelFamily family) {
        return new FamilyDetailResponse(
                family.getId(),
                family.getVendor().getName(),
                family.getFamilyName(),
                family.getCommonDescription(),
                family.getInputTypes(),
                family.getOutputTypes(),
                family.getCreatedAt(),
                family.getUpdatedAt());
    }

    @Override
    public String[] inputTypes() {
        return inputTypes == null ? null : inputTypes.clone();
    }

    @Override
    public String[] outputTypes() {
        return outputTypes == null ? null : outputTypes.clone();
    }
}
