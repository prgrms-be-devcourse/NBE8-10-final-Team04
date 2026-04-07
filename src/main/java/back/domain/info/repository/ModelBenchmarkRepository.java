package back.domain.info.repository;

import back.domain.info.entity.ModelBenchmark;
import back.domain.info.enums.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ModelBenchmarkRepository extends JpaRepository<ModelBenchmark, Long> {
    boolean existsByModelApiIdAndMetricTypeAndMeasuredAt(
            String modelApiId, MetricType metricType, LocalDateTime measuredAt
    );
}
