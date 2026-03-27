package back.domain.info.service;

import back.domain.info.dto.FamilyDto;
import back.domain.info.dto.ModelDto;
import back.domain.info.dto.VendorDto;
import back.domain.info.entity.AiModel;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.mapper.AiModelMapper;
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
    private final AiModelMapper aiModelMapper;
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
                .map(aiModelMapper::toVendorEntity)
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

}
