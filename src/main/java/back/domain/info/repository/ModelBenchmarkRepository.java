package back.domain.info.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import back.domain.info.entity.ModelBenchmark;
import back.domain.info.enums.MetricType;

public interface ModelBenchmarkRepository extends JpaRepository<ModelBenchmark, Long> {
    boolean existsByModelApiIdAndMetricTypeAndMeasuredAt(
            String modelApiId, MetricType metricType, LocalDateTime measuredAt);

    java.util.List<ModelBenchmark> findAllByMeasuredAtBetween(LocalDateTime startInclusive, LocalDateTime endExclusive);
}
