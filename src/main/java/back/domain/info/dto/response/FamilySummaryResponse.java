package back.domain.info.dto.response;

import back.domain.info.entity.AiModelFamily;

public record FamilySummaryResponse(Long id, String familyName, String commonDescription) {

    public static FamilySummaryResponse from(AiModelFamily family) {
        return new FamilySummaryResponse(family.getId(), family.getFamilyName(), family.getCommonDescription());
    }
}
