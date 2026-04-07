package back.domain.info.dto.data;

import back.domain.info.enums.MetricType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ModelBenchmarkDto(
        @JsonProperty("aa_slug")
        String modelApiId,

        @JsonProperty("metric_type")
        MetricType metricType,

        @JsonProperty("metric_value")
        BigDecimal metricValue,

        @JsonProperty("measured_at")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime measuredAt,

        @JsonProperty("unit")
        String unit
) {
}
