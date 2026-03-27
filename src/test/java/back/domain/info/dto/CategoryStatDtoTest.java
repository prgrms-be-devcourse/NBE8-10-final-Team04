package back.domain.info.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import back.domain.info.dto.CategoryStatDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CategoryStatDtoTest {

    @Test
    @DisplayName("DTO를 CategoryStat 엔티티로 변환한다")
    void toEntity_returnsCategoryStat() {
        CategoryStatDto dto = new CategoryStatDto();
        LocalDateTime lastUpdated = LocalDateTime.of(2026, 3, 27, 11, 15);

        ReflectionTestUtils.setField(dto, "category", "coding");
        ReflectionTestUtils.setField(dto, "avgValue", new BigDecimal("87.30"));
        ReflectionTestUtils.setField(dto, "maxValue", new BigDecimal("99.10"));
        ReflectionTestUtils.setField(dto, "minValue", new BigDecimal("65.40"));
        ReflectionTestUtils.setField(dto, "sampleCount", 24);
        ReflectionTestUtils.setField(dto, "lastUpdated", lastUpdated);

        var entity = dto.toEntity();

        assertThat(entity.getCategory()).isEqualTo("coding");
        assertThat(entity.getAvgValue()).isEqualByComparingTo("87.30");
        assertThat(entity.getMaxValue()).isEqualByComparingTo("99.10");
        assertThat(entity.getMinValue()).isEqualByComparingTo("65.40");
        assertThat(entity.getSampleCount()).isEqualTo(24);
        assertThat(entity.getLastUpdated()).isEqualTo(lastUpdated);
    }
}
