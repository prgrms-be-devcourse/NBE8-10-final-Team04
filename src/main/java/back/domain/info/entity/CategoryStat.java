package back.domain.info.entity;

import back.domain.info.dto.CategoryStatDto;
import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "category_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CategoryStat extends BaseEntity {

    @Column(length = 100)
    private String category;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal avgValue;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal maxValue;

    @Column(nullable = false, precision = 15, scale = 6)
    private BigDecimal minValue;

    @Column(nullable = false)
    private Integer sampleCount;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    public void update(CategoryStatDto dto) {
        this.avgValue = dto.getAvgValue();
        this.maxValue = dto.getMaxValue();
        this.minValue = dto.getMinValue();
        this.sampleCount = dto.getSampleCount();
        this.lastUpdated = dto.getLastUpdated();
    }
}
