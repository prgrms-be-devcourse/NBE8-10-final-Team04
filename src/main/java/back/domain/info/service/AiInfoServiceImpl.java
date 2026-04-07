package back.domain.info.service;

import back.domain.info.dto.data.FamilyDto;
import back.domain.info.dto.data.VendorDto;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.mapper.AiModelMapper;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.AiVendorRepository;
import back.global.storage.OciObjectStorageReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다."
)
public class AiInfoServiceImpl implements AiInfoService {

    private final ObjectMapper objectMapper;
    private final OciObjectStorageReader storageReader;
    private final AiVendorRepository aiVendorRepository;
    private final AiModelFamilyRepository aiModelFamilyRepository;
    private final AiModelMapper aiModelMapper;

    private static final String BASE_PATH = "data/ai-info/integrated_major_models.json";

    @Override
    @Transactional
    public void getAiInfo() {
        log.info("[AiInfoService#run] 시작. path={}", BASE_PATH);

        String content = storageReader.readText(BASE_PATH);
        if (content == null) return;

        List<VendorDto> vendorDtos;
        try {
            vendorDtos = objectMapper.readValue(content, new TypeReference<List<VendorDto>>() {});
        } catch (Exception e) {
            log.error("[AiInfoService#run] JSON 파싱 실패", e);
            return;
        }

        int success = 0, fail = 0;
        for (VendorDto dto : vendorDtos) {
            try {
                upsertVendor(dto);
                success++;
            } catch (Exception e) {
                log.error("[AiInfoService#run] vendor 처리 실패, 스킵: {}", dto.name(), e);
                fail++;
            }
        }

        log.info("[AiInfoService#run] 완료. success={}, fail={}", success, fail);
    }

    private void upsertVendor(VendorDto dto) {
        AiVendor vendor = aiVendorRepository.findByName(dto.name()).orElse(null);

        if (vendor == null) {
            vendor = createVendor(dto);
        } else {
            updateVendor(vendor, dto);
        }

        processFamilies(vendor, dto);
    }

    private AiVendor createVendor(VendorDto vendorDto) {
        AiVendor vendor = aiModelMapper.toVendorEntity(vendorDto);
        return aiVendorRepository.save(vendor);
    }

    private void updateVendor(AiVendor vendor, VendorDto vendorDto) {
        vendor.update(
                vendorDto.officialUrl(),
                vendorDto.isActive(),
                vendorDto.isDeprecated()
        );
        log.info("[AiInfoService] vendor 수정: {}", vendor.getName());
    }

    private void processFamilies(AiVendor vendor, VendorDto vendorDto) {
        if (vendorDto.families() == null || vendorDto.families().isEmpty()) {
            return;
        }

        for (FamilyDto familyDto : vendorDto.families()) {
            AiModelFamily family = findFamily(vendor, familyDto.familyName());

            if (family == null) {
                family = createFamily(vendor, familyDto);
            } else {
                updateFamily(family, familyDto);
            }

        }
    }

    private AiModelFamily findFamily(AiVendor vendor, String familyName) {
        return vendor.getModelFamilies().stream()
                .filter(family -> familyName.equals(family.getFamilyName()))
                .findFirst()
                .orElse(null);
    }

    private AiModelFamily createFamily(AiVendor vendor, FamilyDto familyDto) {
        AiModelFamily family = aiModelMapper.toFamilyEntity(familyDto, vendor);
        return aiModelFamilyRepository.save(family);
    }

    private void updateFamily(AiModelFamily family, FamilyDto familyDto) {
        family.update(familyDto.commonDescription(), familyDto.inputTypes(), familyDto.outputTypes());
    }

}
