package back.domain.info.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.info.entity.CategoryStat;
import back.domain.info.entity.ModelBenchmark;
import back.domain.info.enums.MetricType;
import back.domain.info.repository.CategoryStatRepository;
import back.domain.info.repository.ModelBenchmarkRepository;

@ActiveProfiles("test")
@SpringBootTest
class StatServiceImplTest {

    @TempDir
    Path tempDir;

    @Autowired
    private CategoryStatRepository categoryStatRepository;

    @Autowired
    private ModelBenchmarkRepository modelBenchmarkRepository;

    @Autowired
    private StatServiceImpl statService;

    @BeforeEach
    void setUp() {
        modelBenchmarkRepository.deleteAll();
        categoryStatRepository.deleteAll();
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

        statService.run();

        List<CategoryStat> savedCategoryStats = categoryStatRepository.findAll();
        assertThat(savedCategoryStats).hasSize(1);
        assertThat(savedCategoryStats.getFirst().getCategory()).isEqualTo("reasoning");
        assertThat(savedCategoryStats.getFirst().getSampleCount()).isEqualTo(12);
        assertThat(savedCategoryStats.getFirst().getLastUpdated())
                .isEqualTo(LocalDateTime.of(2026, 3, 27, 9, 0));

        List<ModelBenchmark> savedBenchmarks = modelBenchmarkRepository.findAll();
        assertThat(savedBenchmarks).hasSize(2);
        assertThat(savedBenchmarks)
                .extracting(ModelBenchmark::getMetricType)
                .containsExactly(MetricType.CODING, MetricType.PRICE_BLENDED);
        assertThat(savedBenchmarks)
                .extracting(ModelBenchmark::getModelApiId)
                .containsExactly("gpt-4.1", "claude-sonnet");
    }

    @Test
    @DisplayName("카테고리 통계 파일을 읽지 못하면 IllegalStateException을 던진다")
    void run_whenCategoryStatFileMissing_throwsException() {
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

        assertThatThrownBy(() -> statService.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing-category-stats.json");

        assertThat(modelBenchmarkRepository.findAll()).isEmpty();
    }

    private Path writeJson(String fileName, String json) throws IOException {
        Path jsonFile = tempDir.resolve(fileName);
        Files.writeString(jsonFile, json);
        return jsonFile;
    }
}
