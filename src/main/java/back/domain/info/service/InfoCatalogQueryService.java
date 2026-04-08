package back.domain.info.service;

import java.util.List;

import back.domain.info.dto.response.FamilyDetailResponse;
import back.domain.info.dto.response.FamilySummaryResponse;
import back.domain.info.dto.response.VendorInfoResponse;

public interface InfoCatalogQueryService {

    List<VendorInfoResponse> getVendors();

    List<FamilySummaryResponse> getFamiliesByVendorId(long vendorId);

    FamilyDetailResponse getFamilyDetail(long familyId);
}
