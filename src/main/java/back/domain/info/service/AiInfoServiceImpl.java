package back.domain.info.service;

import back.domain.info.dto.FamilyDto;
import back.domain.info.dto.ModelDto;
import back.domain.info.dto.VendorDto;
import back.domain.info.entity.AiModel;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.repository.AiVendorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInfoServiceImpl implements AiInfoService {

    @Value("${app.info.json-path:data/ai-info/integrated_major_models.json}")
    private String jsonFilePath;

    private final AiVendorRepository aiVendorRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public void run() {

        List<VendorDto> vendorDtos = readJson(jsonFilePath);
        if (vendorDtos == null || vendorDtos.isEmpty()) {
            log.warn("[DataSeedService] JSON 파일에서 읽은 데이터가 없습니다.");
            return;
        }

        List<AiVendor> vendors = vendorDtos.stream()
                .map(this::toVendorEntity)
                .toList();

        aiVendorRepository.saveAll(vendors);
        log.info("[DataSeedService] {}개 벤더 적재 완료.", vendors.size());
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

    private AiVendor toVendorEntity(VendorDto dto) {
        AiVendor vendor = AiVendor.builder()
                .name(dto.getName())
                .officialUrl(dto.getOfficialUrl())
                .isActive(dto.getIsActive())
                .isDeprecated(dto.getIsDeprecated())
                .modelFamilies(new ArrayList<>())
                .build();

        if (dto.getFamilies() != null) {
            dto.getFamilies().stream()
                    .map(f -> toFamilyEntity(f, vendor))
                    .forEach(vendor.getModelFamilies()::add);
        }

        return vendor;
    }

    private AiModelFamily toFamilyEntity(FamilyDto dto, AiVendor vendor) {
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName(dto.getFamilyName())
                .commonDescription(dto.getCommonDescription())
                .models(new ArrayList<>())
                .build();

        if (dto.getModels() != null) {
            dto.getModels().stream()
                    .map(m -> toModelEntity(m, family))
                    .forEach(family.getModels()::add);
        }

        return family;
    }

    private AiModel toModelEntity(ModelDto dto, AiModelFamily family) {
        return AiModel.builder()
                .family(family)
                .modelName(dto.getModelName())
                .apiId(dto.getApiId())
                .contextWindow(dto.getContextWindow())
                .maxOutputTokens(dto.getMaxOutputTokens())
                .releaseDate(parseDate(dto.getReleaseDate()))
                .isPreview(dto.getIsPreview())
                .modelImageUrl(dto.getModelImageUrl())
                .inputPrice(dto.getInputPrice())
                .outputPrice(dto.getOutputPrice())
                .inputModalities(dto.getInputModalities() != null ? dto.getInputModalities() : new ArrayList<>())
                .outputModalities(dto.getOutputModalities() != null ? dto.getOutputModalities() : new ArrayList<>())
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("[DataSeedService] 날짜 파싱 실패 (값: {}), null로 처리합니다.", dateStr);
            return null;
        }
    }
}
