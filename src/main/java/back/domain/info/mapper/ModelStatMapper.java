package back.domain.info.mapper;

import back.domain.info.dto.CategoryStatDto;
import back.domain.info.dto.ModelBenchmarkDto;
import back.domain.info.entity.CategoryStat;
import back.domain.info.entity.ModelBenchmark;
import org.springframework.stereotype.Component;

@Component
public class ModelStatMapper {
    public ModelBenchmark toModelBenchmarkEntity(ModelBenchmarkDto dto) {
        return ModelBenchmark.builder()
                .modelApiId(dto.getModelApiId())
                .metricType(dto.getMetricType())
                .metricValue(dto.getMetricValue())
                .measuredAt(dto.getMeasuredAt())
                .unit(dto.getUnit())
                .build();
    }

    public CategoryStat toCategoryStatEntity(CategoryStatDto dto) {
        return CategoryStat.builder()
                .category(dto.getCategory())
                .avgValue(dto.getAvgValue())
                .maxValue(dto.getMaxValue())
                .minValue(dto.getMinValue())
                .sampleCount(dto.getSampleCount())
                .lastUpdated(dto.getLastUpdated())
                .build();
    }
}
