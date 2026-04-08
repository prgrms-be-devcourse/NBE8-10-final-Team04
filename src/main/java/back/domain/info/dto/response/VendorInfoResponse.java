package back.domain.info.dto.response;

import back.domain.info.entity.AiVendor;

public record VendorInfoResponse(Long id, String name, String officialUrl, Boolean active, Boolean deprecated) {

    public static VendorInfoResponse from(AiVendor vendor) {
        return new VendorInfoResponse(
                vendor.getId(),
                vendor.getName(),
                vendor.getOfficialUrl(),
                vendor.getIsActive(),
                vendor.getIsDeprecated());
    }
}
