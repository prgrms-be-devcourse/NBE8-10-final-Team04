package back.domain.info.service;

import back.domain.info.dto.data.ModelBenchmarkDto;
import back.domain.info.entity.ModelBenchmark;
import back.domain.info.mapper.ModelStatMapper;
import back.domain.info.repository.ModelBenchmarkRepository;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
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
        log.info("[ModelBenchmarkService#run] 시작. path={}", BASE_PATH);

        String content = storageReader.readText(BASE_PATH);
        if (content == null) return;

        List<ModelBenchmarkDto> benchmarkDtos;
        try {
            benchmarkDtos = objectMapper.readValue(content, new TypeReference<List<ModelBenchmarkDto>>() {});
        } catch (Exception e) {
            log.error("[ModelBenchmarkService#run] JSON 파싱 실패", e);
            return;
        }

        int success = 0;
        for(ModelBenchmarkDto dto : benchmarkDtos) {
            try {
                createModelBenchmark(dto);
                success++;
            } catch (Exception e) {
                throw new ServiceException(
                        CommonErrorCode.INTERNAL_SERVER_ERROR,
                        "[ModelBenchmarkService#run] Failed to create model benchmark. (modelApiId=" + dto.modelApiId() + ")",
                        "모델 벤치마크 데이터 생성 중 오류가 발생했습니다. (modelApiId=" + dto.modelApiId() + ")"
                );
            }
        }

        log.info("[ModelBenchmarkService#run] 완료. success={}", success);
    }

    private ModelBenchmark createModelBenchmark(ModelBenchmarkDto dto) {
        ModelBenchmark benchmark = modelStatMapper.toModelBenchmarkEntity(dto);
        ModelBenchmark savedBenchmark = benchmarkRepository.save(benchmark);
        return savedBenchmark;
    }

}
