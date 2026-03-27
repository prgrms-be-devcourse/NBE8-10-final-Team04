package back.domain.info.service;

import back.domain.info.dto.CategoryStatDto;
import back.domain.info.dto.ModelBenchmarkDto;
import back.domain.info.dto.VendorDto;
import back.domain.info.entity.AiModel;
import back.domain.info.entity.AiVendor;
import back.domain.info.entity.CategoryStat;
import back.domain.info.entity.ModelBenchmark;
import back.domain.info.repository.AiModelRepository;
import back.domain.info.repository.CategoryStatRepository;
import back.domain.info.repository.ModelBenchmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    @Value("${app.info.category-stat-path:data/stats/category_stats.json}")
    private String categoryStatPath;

    @Value("${app.info.model-benchmark-path:data/stats/model_benchmarks_records.json}")
    private String modelBenchmarkPath;

    private final CategoryStatRepository categoryStatRepository;
    private final ModelBenchmarkRepository modelBenchmarkRepository;
    private final AiModelRepository aiModelRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run() {
        readCategoryStat();
        readModelBenchmark();
    }

    private void readCategoryStat() {
        List<CategoryStatDto> statDtos = readJson(
                categoryStatPath,
                new TypeReference<List<CategoryStatDto>>() {}
        );

        if (statDtos.isEmpty()) {
            log.warn("[StatService] category_stats JSON 파일에서 읽은 데이터가 없습니다.");
            return;
        }

        List<CategoryStat> stats = statDtos.stream()
                .map(CategoryStatDto::toEntity)
                .toList();

        categoryStatRepository.saveAll(stats);
        log.info("[StatService] category_stats {}개 적재 완료.", stats.size());
    }

    private void readModelBenchmark() {
        List<ModelBenchmarkDto> benchmarkDtos = readJson(
                modelBenchmarkPath,
                new TypeReference<List<ModelBenchmarkDto>>() {}
        );

        if (benchmarkDtos.isEmpty()) {
            log.warn("[StatService] model_benchmarks JSON 파일에서 읽은 데이터가 없습니다.");
            return;
        }

        List<ModelBenchmark> benchmarks = benchmarkDtos.stream()
                .map(this::toModelBenchmarkEntity)
                .toList();

        modelBenchmarkRepository.saveAll(benchmarks);
        log.info("[StatService] model_benchmarks {}개 적재 완료.", benchmarks.size());
    }

    private ModelBenchmark toModelBenchmarkEntity(ModelBenchmarkDto dto) {
        return modelBenchmarkRepository.save(
                ModelBenchmark.builder()
                        .modelApiId(dto.getModelApiId())
                        .metricType(dto.getMetricType())
                        .metricValue(dto.getMetricValue())
                        .measuredAt(dto.getMeasuredAt())
                        .unit(dto.getUnit())
                        .build()
        );
    }

    private <T> T readJson(String path, TypeReference<T> typeReference) {
        try {
            log.info("[StatService] JSON을 읽습니다: {}", path);
            String json = Files.readString(Path.of(path));
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("[StatService] JSON 읽기 실패: {}", path, e);
            throw new IllegalStateException("JSON 읽기 실패: " + path, e);
        }
    }
}
