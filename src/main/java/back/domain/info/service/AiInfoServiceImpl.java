package back.domain.info.service;

import back.domain.info.dto.FamilyDto;
import back.domain.info.dto.ModelDto;
import back.domain.info.dto.VendorDto;
import back.domain.info.entity.AiModel;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.mapper.AiModelMapper;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.AiModelRepository;
import back.domain.info.repository.AiVendorRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다.")
public class AiInfoServiceImpl implements AiInfoService {

    @Value("${app.info.json-path:data/ai-info/integrated_major_models.json}")
    private String jsonFilePath;

    private final AiVendorRepository aiVendorRepository;
    private final AiModelFamilyRepository aiModelFamilyRepository;
    private final AiModelRepository aiModelRepository;
    private final AiModelMapper aiModelMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public void run() {

        List<VendorDto> vendorDtos = readJson(jsonFilePath);
        if (vendorDtos == null || vendorDtos.isEmpty()) {
            log.warn("[AiInfoService] JSON 파일에서 읽은 데이터가 없습니다.");
            return;
        }

        int createdVendorCount = 0;
        int updatedVendorCount = 0;

        for (VendorDto vendorDto : vendorDtos) {
            AiVendor vendor = aiVendorRepository.findByName(vendorDto.getName())
                    .orElse(null);

            if (vendor == null) {
                vendor = createVendor(vendorDto);
                createdVendorCount++;
            } else {
                updateVendor(vendor, vendorDto);
                updatedVendorCount++;
            }

            processFamilies(vendor, vendorDto);
        }

        log.info(
                "[AiInfoServiceImpl] vendor upsert 완료. created={}, updated={}, total={}",
                createdVendorCount,
                updatedVendorCount,
                vendorDtos.size()
        );
    }

    private AiVendor createVendor(VendorDto vendorDto) {
        AiVendor vendor = aiModelMapper.toVendorEntity(vendorDto);
        AiVendor savedVendor = aiVendorRepository.save(vendor);
        return savedVendor;
    }

    private void updateVendor(AiVendor vendor, VendorDto vendorDto) {
        vendor.update(
                vendorDto.getOfficialUrl(),
                vendorDto.getIsActive(),
                vendorDto.getIsDeprecated()
        );
        log.info("[AiInfoService] vendor 수정: {}", vendor.getName());
    }

    private void processFamilies(AiVendor vendor, VendorDto vendorDto) {
        if (vendorDto.getFamilies() == null || vendorDto.getFamilies().isEmpty()) {
            return;
        }

        for (FamilyDto familyDto : vendorDto.getFamilies()) {
            AiModelFamily family = findFamily(vendor, familyDto.getFamilyName());

            if (family == null) {
                family = createFamily(vendor, familyDto);
            } else {
                updateFamily(family, familyDto);
            }

            processModels(family, familyDto);
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
        family.update(familyDto.getCommonDescription());
    }

    private void processModels(AiModelFamily family, FamilyDto familyDto) {
        if (familyDto.getModels() == null || familyDto.getModels().isEmpty()) {
            return;
        }

        for (ModelDto modelDto : familyDto.getModels()) {
            AiModel model = aiModelRepository.findByApiId(modelDto.getApiId())
                    .orElse(null);

            if (model == null) {
                createModel(family, modelDto);
            } else {
                updateModel(model, modelDto);
            }
        }
    }

    private AiModel createModel(AiModelFamily family, ModelDto modelDto) {
        AiModel model = aiModelMapper.toModelEntity(modelDto, family);
        return aiModelRepository.save(model);
    }

    private void updateModel(AiModel model, ModelDto modelDto) {
        model.update(
                modelDto.getModelName(),
                modelDto.getContextWindow(),
                modelDto.getMaxOutputTokens(),
                modelDto.getReleaseDate(),
                modelDto.getIsPreview(),
                modelDto.getModelImageUrl(),
                modelDto.getInputPrice(),
                modelDto.getOutputPrice(),
                modelDto.getInputModalities(),
                modelDto.getOutputModalities()
        );
    }

    private List<VendorDto> readJson(String path) {
        try {
            log.info("[DataSeedService] JSON을 읽습니다: {}", path);
            String json = java.nio.file.Files.readString(java.nio.file.Path.of(path));
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            log.error("[DataSeedService] JSON 읽기 실패: {}", path, e);
            return List.of();
        }
    }

    // OCI Object Storage 연동 시 이 메서드만 교체
    private List<VendorDto> readFromOci(String bucket, String objectName) throws IOException {
        // TODO: OCI SDK 연동
        return List.of();
    }

}
