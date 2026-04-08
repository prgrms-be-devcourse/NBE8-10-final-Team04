package back.domain.info.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import back.domain.info.entity.ModelBenchmark;
import back.domain.info.enums.MetricType;

public record BenchmarkMetricView(
        String modelApiId, MetricType metricType, BigDecimal metricValue, String unit, LocalDateTime measuredAt) {

    public static BenchmarkMetricView from(ModelBenchmark benchmark) {
        return new BenchmarkMetricView(
                benchmark.getModelApiId(),
                benchmark.getMetricType(),
                benchmark.getMetricValue(),
                benchmark.getUnit(),
                benchmark.getMeasuredAt());
    }
}
