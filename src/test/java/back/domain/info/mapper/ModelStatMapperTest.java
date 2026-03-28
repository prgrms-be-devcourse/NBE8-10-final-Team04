package back.domain.info.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.info.dto.CategoryStatDto;
import back.domain.info.dto.ModelBenchmarkDto;
import back.domain.info.enums.MetricType;

class ModelStatMapperTest {

    private final ModelStatMapper mapper = new ModelStatMapper();

    @Test
    @DisplayName("CategoryStatDto를 CategoryStat 엔티티로 변환한다")
    void toCategoryStatEntity_mapsFields() {
        CategoryStatDto dto = new CategoryStatDto();
        LocalDateTime lastUpdated = LocalDateTime.of(2026, 3, 27, 9, 0);

        ReflectionTestUtils.setField(dto, "category", "reasoning");
        ReflectionTestUtils.setField(dto, "avgValue", new BigDecimal("85.50"));
        ReflectionTestUtils.setField(dto, "maxValue", new BigDecimal("99.90"));
        ReflectionTestUtils.setField(dto, "minValue", new BigDecimal("70.10"));
        ReflectionTestUtils.setField(dto, "sampleCount", 12);
        ReflectionTestUtils.setField(dto, "lastUpdated", lastUpdated);

        var entity = mapper.toCategoryStatEntity(dto);

        assertThat(entity.getCategory()).isEqualTo("reasoning");
        assertThat(entity.getAvgValue()).isEqualByComparingTo("85.50");
        assertThat(entity.getMaxValue()).isEqualByComparingTo("99.90");
        assertThat(entity.getMinValue()).isEqualByComparingTo("70.10");
        assertThat(entity.getSampleCount()).isEqualTo(12);
        assertThat(entity.getLastUpdated()).isEqualTo(lastUpdated);
    }

    @Test
    @DisplayName("ModelBenchmarkDto를 ModelBenchmark 엔티티로 변환한다")
    void toModelBenchmarkEntity_mapsFields() {
        ModelBenchmarkDto dto = new ModelBenchmarkDto();
        LocalDateTime measuredAt = LocalDateTime.of(2026, 3, 27, 9, 30);

        ReflectionTestUtils.setField(dto, "modelApiId", "gpt-4.1");
        ReflectionTestUtils.setField(dto, "metricType", MetricType.CODING);
        ReflectionTestUtils.setField(dto, "metricValue", new BigDecimal("92.30"));
        ReflectionTestUtils.setField(dto, "measuredAt", measuredAt);
        ReflectionTestUtils.setField(dto, "unit", "score");

        var entity = mapper.toModelBenchmarkEntity(dto);

        assertThat(entity.getModelApiId()).isEqualTo("gpt-4.1");
        assertThat(entity.getMetricType()).isEqualTo(MetricType.CODING);
        assertThat(entity.getMetricValue()).isEqualByComparingTo("92.30");
        assertThat(entity.getMeasuredAt()).isEqualTo(measuredAt);
        assertThat(entity.getUnit()).isEqualTo("score");
    }
}
