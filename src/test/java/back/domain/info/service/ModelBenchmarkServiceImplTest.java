package back.domain.info.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.info.entity.ModelBenchmark;
import back.domain.info.enums.MetricType;
import back.domain.info.mapper.ModelStatMapper;
import back.domain.info.repository.ModelBenchmarkRepository;
import tools.jackson.databind.ObjectMapper;

class BenchmarkServiceImplTest {

    @TempDir
    Path tempDir;

    private BenchmarkServiceImpl statService;

    private CategoryStatRepository categoryStatRepository;

    private ModelBenchmarkRepository modelBenchmarkRepository;

    private AiModelRepository aiModelRepository;

    @BeforeEach
    void setUp() {
        categoryStatRepository = mock(CategoryStatRepository.class);
        modelBenchmarkRepository = mock(ModelBenchmarkRepository.class);
        aiModelRepository = mock(AiModelRepository.class);
        statService = new BenchmarkServiceImpl(
                categoryStatRepository,
                modelBenchmarkRepository,
                aiModelRepository,
                new ModelStatMapper(),
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("카테고리 통계와 모델 벤치마크 JSON을 읽어 각각 저장한다")
    void run_savesCategoryStatsAndBenchmarks() throws IOException {
        Path categoryStatsFile = writeJson(
                "category-stats.json",
                """
                [
                  {
                    "category": "reasoning",
                    "avg_value": 85.50,
                    "max_value": 99.90,
                    "min_value": 70.10,
                    "sample_count": 12,
                    "last_updated": "2026-03-27 09:00:00"
                  }
                ]
                """
        );
        Path benchmarksFile = writeJson(
                "benchmarks.json",
                """
                [
                  {
                    "model_api_id": "gpt-4.1",
                    "metric_type": "CODING",
                    "metric_value": 92.30,
                    "measured_at": "2026-03-27 09:30:00",
                    "unit": "score"
                  },
                  {
                    "model_api_id": "claude-sonnet",
                    "metric_type": "PRICE_BLENDED",
                    "metric_value": 1.50,
                    "measured_at": "2026-03-27 10:00:00",
                    "unit": "usd"
                  }
                ]
                """
        );

        ReflectionTestUtils.setField(statService, "categoryStatPath", categoryStatsFile.toString());
        ReflectionTestUtils.setField(statService, "modelBenchmarkPath", benchmarksFile.toString());
        when(categoryStatRepository.findByCategory("reasoning")).thenReturn(java.util.Optional.empty());
        when(categoryStatRepository.save(any(CategoryStat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelBenchmarkRepository.findByModelApiIdAndMetricType("gpt-4.1", MetricType.CODING))
                .thenReturn(java.util.Optional.empty());
        when(modelBenchmarkRepository.findByModelApiIdAndMetricType("claude-sonnet", MetricType.PRICE_BLENDED))
                .thenReturn(java.util.Optional.empty());
        when(modelBenchmarkRepository.save(any(ModelBenchmark.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        statService.run();

        ArgumentCaptor<CategoryStat> categoryCaptor = ArgumentCaptor.forClass(CategoryStat.class);
        verify(categoryStatRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getCategory()).isEqualTo("reasoning");
        assertThat(categoryCaptor.getValue().getSampleCount()).isEqualTo(12);
        assertThat(categoryCaptor.getValue().getLastUpdated())
                .isEqualTo(LocalDateTime.of(2026, 3, 27, 9, 0));

        ArgumentCaptor<ModelBenchmark> benchmarkCaptor = ArgumentCaptor.forClass(ModelBenchmark.class);
        verify(modelBenchmarkRepository, org.mockito.Mockito.times(2)).save(benchmarkCaptor.capture());
        assertThat(benchmarkCaptor.getAllValues())
                .extracting(ModelBenchmark::getMetricType)
                .containsExactly(MetricType.CODING, MetricType.PRICE_BLENDED);
        assertThat(benchmarkCaptor.getAllValues())
                .extracting(ModelBenchmark::getModelApiId)
                .containsExactly("gpt-4.1", "claude-sonnet");
    }

    @Test
    @DisplayName("카테고리 통계 파일을 읽지 못하면 저장 로직을 수행하지 않는다")
    void run_whenCategoryStatFileMissing_doesNothing() {
        ReflectionTestUtils.setField(
                statService,
                "categoryStatPath",
                tempDir.resolve("missing-category-stats.json").toString()
        );
        ReflectionTestUtils.setField(
                statService,
                "modelBenchmarkPath",
                tempDir.resolve("unused-benchmarks.json").toString()
        );

        statService.run();

        verifyNoInteractions(categoryStatRepository, modelBenchmarkRepository, aiModelRepository);
    }

    @Test
    @DisplayName("이미 존재하는 카테고리 통계와 벤치마크는 값을 갱신한다")
    void run_updatesExistingStatsAndBenchmarks() throws IOException {
        Path categoryStatsFile = writeJson(
                "category-stats.json",
                """
                [
                  {
                    "category": "reasoning",
                    "avg_value": 85.50,
                    "max_value": 99.90,
                    "min_value": 70.10,
                    "sample_count": 12,
                    "last_updated": "2026-03-27 09:00:00"
                  }
                ]
                """
        );
        Path benchmarksFile = writeJson(
                "benchmarks.json",
                """
                [
                  {
                    "model_api_id": "gpt-4.1",
                    "metric_type": "CODING",
                    "metric_value": 92.30,
                    "measured_at": "2026-03-27 09:30:00",
                    "unit": "score"
                  }
                ]
                """
        );

        CategoryStat existingStat = CategoryStat.builder()
                .category("reasoning")
                .avgValue(new java.math.BigDecimal("1.00"))
                .maxValue(new java.math.BigDecimal("2.00"))
                .minValue(new java.math.BigDecimal("0.50"))
                .sampleCount(1)
                .lastUpdated(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();

        ModelBenchmark existingBenchmark = ModelBenchmark.builder()
                .modelApiId("gpt-4.1")
                .metricType(MetricType.CODING)
                .metricValue(new java.math.BigDecimal("10.00"))
                .measuredAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .unit("old")
                .build();

        ReflectionTestUtils.setField(statService, "categoryStatPath", categoryStatsFile.toString());
        ReflectionTestUtils.setField(statService, "modelBenchmarkPath", benchmarksFile.toString());
        when(categoryStatRepository.findByCategory("reasoning")).thenReturn(java.util.Optional.of(existingStat));
        when(modelBenchmarkRepository.findByModelApiIdAndMetricType("gpt-4.1", MetricType.CODING))
                .thenReturn(java.util.Optional.of(existingBenchmark));

        statService.run();

        assertThat(existingStat.getAvgValue()).isEqualByComparingTo("85.50");
        assertThat(existingStat.getMaxValue()).isEqualByComparingTo("99.90");
        assertThat(existingStat.getMinValue()).isEqualByComparingTo("70.10");
        assertThat(existingStat.getSampleCount()).isEqualTo(12);
        assertThat(existingStat.getLastUpdated()).isEqualTo(LocalDateTime.of(2026, 3, 27, 9, 0));

        assertThat(existingBenchmark.getMetricValue()).isEqualByComparingTo("92.30");
        assertThat(existingBenchmark.getMeasuredAt()).isEqualTo(LocalDateTime.of(2026, 3, 27, 9, 30));
        assertThat(existingBenchmark.getUnit()).isEqualTo("score");

        verify(categoryStatRepository, never()).save(any(CategoryStat.class));
        verify(modelBenchmarkRepository, never()).save(any(ModelBenchmark.class));
    }

    private Path writeJson(String fileName, String json) throws IOException {
        Path jsonFile = tempDir.resolve(fileName);
        Files.writeString(jsonFile, json);
        return jsonFile;
    }
}
