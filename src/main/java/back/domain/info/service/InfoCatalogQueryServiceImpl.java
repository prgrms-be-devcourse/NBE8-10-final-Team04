package back.domain.info.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import back.domain.info.dto.response.FamilyDetailResponse;
import back.domain.info.dto.response.FamilySummaryResponse;
import back.domain.info.dto.response.VendorInfoResponse;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.AiVendorRepository;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "스프링 DI로 주입되는 빈 참조이며 의도된 패턴이다.")
@Transactional(readOnly = true)
public class InfoCatalogQueryServiceImpl implements InfoCatalogQueryService {

    private final AiVendorRepository aiVendorRepository;
    private final AiModelFamilyRepository aiModelFamilyRepository;

    @Override
    public List<VendorInfoResponse> getVendors() {
        return aiVendorRepository.findAllByOrderByNameAsc().stream()
                .map(VendorInfoResponse::from)
                .toList();
    }

    @Override
    public List<FamilySummaryResponse> getFamiliesByVendorId(long vendorId) {
        return aiModelFamilyRepository.findAllByVendorIdOrderByFamilyNameAsc(vendorId).stream()
                .map(FamilySummaryResponse::from)
                .toList();
    }

    @Override
    public FamilyDetailResponse getFamilyDetail(long familyId) {
        AiModelFamily family = aiModelFamilyRepository
                .findWithVendorById(familyId)
                .orElseThrow(() -> new ServiceException(
                        CommonErrorCode.NOT_FOUND,
                        "[InfoCatalogQueryServiceImpl#getFamilyDetail] family not found",
                        "패밀리 정보를 찾을 수 없습니다."));

        return FamilyDetailResponse.from(family);
    }
}
