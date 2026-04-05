package back.domain.info.mapper;

import back.domain.info.dto.data.ModelBenchmarkDto;
import back.domain.info.enums.MetricType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ModelStatMapperTest {

    private final ModelStatMapper mapper = new ModelStatMapper();

    @Test
    void toModelBenchmarkEntity_mapsFields() {
        LocalDateTime measuredAt = LocalDateTime.of(2026, 3, 27, 9, 30);
        ModelBenchmarkDto dto = new ModelBenchmarkDto(
                "gpt-4.1",
                MetricType.CODING,
                new BigDecimal("92.30"),
                measuredAt,
                "score"
        );

        var entity = mapper.toModelBenchmarkEntity(dto);

        assertThat(entity.getModelApiId()).isEqualTo("gpt-4.1");
        assertThat(entity.getMetricType()).isEqualTo(MetricType.CODING);
        assertThat(entity.getMetricValue()).isEqualByComparingTo("92.30");
        assertThat(entity.getMeasuredAt()).isEqualTo(measuredAt);
        assertThat(entity.getUnit()).isEqualTo("score");
    }
}
