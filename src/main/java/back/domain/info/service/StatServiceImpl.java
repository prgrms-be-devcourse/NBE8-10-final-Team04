package back.domain.info.service;

import back.domain.info.dto.CategoryStatDto;
import back.domain.info.dto.ModelBenchmarkDto;
import back.domain.info.entity.CategoryStat;
import back.domain.info.entity.ModelBenchmark;
import back.domain.info.mapper.ModelStatMapper;
import back.domain.info.repository.AiModelRepository;
import back.domain.info.repository.CategoryStatRepository;
import back.domain.info.repository.ModelBenchmarkRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.transaction.Transactional;
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
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다.")
public class StatServiceImpl implements StatService {

    @Value("${app.info.category-stat-path:data/stats/category_stats.json}")
    private String categoryStatPath;

    @Value("${app.info.model-benchmark-path:data/stats/model_benchmarks_records.json}")
    private String modelBenchmarkPath;

    private final CategoryStatRepository categoryStatRepository;
    private final ModelBenchmarkRepository modelBenchmarkRepository;
    private final AiModelRepository aiModelRepository;
    private final ModelStatMapper modelStatMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run() {
        processCategoryStats();
        processModelBenchmarks();
    }

    private void processCategoryStats() {
        List<CategoryStatDto> statDtos = readJson(
                categoryStatPath,
                new TypeReference<List<CategoryStatDto>>() {}
        );

        if (statDtos == null || statDtos.isEmpty()) {
            log.warn("[StatService] category_stats JSON 파일에서 읽은 데이터가 없습니다.");
            return;
        }

        int createdCount = 0;
        int updatedCount = 0;

        for (CategoryStatDto statDto : statDtos) {
            CategoryStat stat = categoryStatRepository.findByCategory(statDto.getCategory())
                    .orElse(null);

            if (stat == null) {
                createCategoryStat(statDto);
                createdCount++;
            } else {
                updateCategoryStat(stat, statDto);
                updatedCount++;
            }
        }

        log.info(
                "[StatService] category_stats upsert 완료. created={}, updated={}, total={}",
                createdCount,
                updatedCount,
                statDtos.size()
        );
    }

    private void processModelBenchmarks() {
        List<ModelBenchmarkDto> benchmarkDtos = readJson(
                modelBenchmarkPath,
                new TypeReference<List<ModelBenchmarkDto>>() {}
        );

        if (benchmarkDtos == null || benchmarkDtos.isEmpty()) {
            log.warn("[StatService] model_benchmarks JSON 파일에서 읽은 데이터가 없습니다.");
            return;
        }

        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        for (ModelBenchmarkDto benchmarkDto : benchmarkDtos) {

            ModelBenchmark benchmark = modelBenchmarkRepository
                    .findByModelApiIdAndMetricType(benchmarkDto.getModelApiId(), benchmarkDto.getMetricType())
                    .orElse(null);

            if (benchmark == null) {
                createModelBenchmark(benchmarkDto);
                createdCount++;
            } else {
                updateModelBenchmark(benchmark, benchmarkDto);
                updatedCount++;
            }
        }

        log.info(
                "[StatService] model_benchmarks upsert 완료. created={}, updated={}, skipped={}, total={}",
                createdCount,
                updatedCount,
                skippedCount,
                benchmarkDtos.size()
        );
    }

    private CategoryStat createCategoryStat(CategoryStatDto dto) {
        CategoryStat stat = modelStatMapper.toCategoryStatEntity(dto);
        CategoryStat savedStat = categoryStatRepository.save(stat);
        return savedStat;
    }

    private void updateCategoryStat(CategoryStat stat, CategoryStatDto dto) {
        stat.update(dto);
        log.info("[StatService] category_stat 수정: {}", stat.getCategory());
    }

    private ModelBenchmark createModelBenchmark(ModelBenchmarkDto dto) {
        ModelBenchmark benchmark = modelStatMapper.toModelBenchmarkEntity(dto);
        ModelBenchmark savedBenchmark = modelBenchmarkRepository.save(benchmark);
        return savedBenchmark;
    }

    private void updateModelBenchmark(ModelBenchmark benchmark, ModelBenchmarkDto dto) {
        benchmark.update(dto);
        log.info(
                "[StatService] model_benchmark 수정: modelApiId={}, metricType={}",
                benchmark.getModelApiId(),
                benchmark.getMetricType()
        );
    }

    private <T> T readJson(String path, TypeReference<T> typeReference) {
        try {
            String json = Files.readString(Path.of(path));
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("[StatService] JSON 읽기 실패: {}", path, e);
            return null;
        }
    }
}
