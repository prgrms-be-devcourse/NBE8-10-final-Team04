package back.domain.info.service;

import back.domain.info.dto.data.ModelBenchmarkDto;
import back.domain.info.entity.ModelBenchmark;
import back.domain.info.mapper.ModelStatMapper;
import back.domain.info.repository.ModelBenchmarkRepository;
import back.global.storage.OciObjectStorageReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다.")
public class ModelBenchmarkServiceImpl implements ModelBenchmarkService {

    private static final String BASE_PATH = "data/ai-info/model_benchmarks_records.json";

    private final ObjectMapper objectMapper;
    private final OciObjectStorageReader storageReader;
    private final ModelBenchmarkRepository benchmarkRepository;
    private final ModelStatMapper modelStatMapper;

    @Override
    @Transactional
    public void run() {
        String content = storageReader.readText(BASE_PATH);
        if (content == null) return;

        List<ModelBenchmarkDto> benchmarkDtos;
        try {
            benchmarkDtos = objectMapper.readValue(content, new TypeReference<List<ModelBenchmarkDto>>() {});
        } catch (Exception e) {
            log.error("[ModelBenchmarkServiceImpl] JSON 파싱 실패", e);
            return;
        }

        int success = 0, fail = 0;
        for(ModelBenchmarkDto dto : benchmarkDtos) {
            try {
                upsertModelBenchmark(dto);
                success++;
            } catch (Exception e) {
                log.error("[ModelBenchmarkServiceImpl] 모델 벤치마크 업서트 실패: {}", dto, e);
                fail++; // 이 dto만 롤백, 나머지 계속 진행
            }
        }

        log.info("[ModelBenchmarkService] 완료. success={}, fail={}", success, fail);
    }

    private void upsertModelBenchmark(ModelBenchmarkDto dto) {

        ModelBenchmark benchmark = benchmarkRepository
                .findByModelApiIdAndMetricType(dto.getModelApiId(), dto.getMetricType())
                .orElse(null);

        if (benchmark == null) {
            createModelBenchmark(dto);
        } else {
            updateModelBenchmark(benchmark, dto);
        }

    }

    private ModelBenchmark createModelBenchmark(ModelBenchmarkDto dto) {
        ModelBenchmark benchmark = modelStatMapper.toModelBenchmarkEntity(dto);
        ModelBenchmark savedBenchmark = benchmarkRepository.save(benchmark);
        return savedBenchmark;
    }

    private void updateModelBenchmark(ModelBenchmark benchmark, ModelBenchmarkDto dto) {
        benchmark.update(dto);
    }

}
