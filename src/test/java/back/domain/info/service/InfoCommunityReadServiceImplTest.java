package back.domain.info.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import back.domain.info.dto.response.BenchmarkMetricView;
import back.domain.info.dto.response.ModelInfoFamilyView;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.entity.ModelBenchmark;
import back.domain.info.enums.MetricType;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.ModelBenchmarkRepository;

class InfoCommunityReadServiceImplTest {

    private AiModelFamilyRepository aiModelFamilyRepository;
    private ModelBenchmarkRepository modelBenchmarkRepository;
    private InfoCommunityReadService infoCommunityReadService;

    @BeforeEach
    void setUp() {
        aiModelFamilyRepository = mock(AiModelFamilyRepository.class);
        modelBenchmarkRepository = mock(ModelBenchmarkRepository.class);
        infoCommunityReadService = new InfoCommunityReadServiceImpl(aiModelFamilyRepository, modelBenchmarkRepository);
    }

    @Test
    void getModelInfoByDate_returnsMappedResult() {
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        AiVendor vendor = AiVendor.builder().name("OpenAI").isActive(true).isDeprecated(false).build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT-5.4")
                .commonDescription("desc")
                .inputTypes(new String[] {"text"})
                .outputTypes(new String[] {"text"})
                .build();
        when(aiModelFamilyRepository.findAllChangedBetween(targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(family));

        List<ModelInfoFamilyView> result = infoCommunityReadService.getModelInfoByDate(targetDate);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().vendorName()).isEqualTo("OpenAI");
        assertThat(result.getFirst().familyName()).isEqualTo("GPT-5.4");
    }

    @Test
    void getModelInfoByDateAndVendor_returnsMappedResult() {
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        Long vendorId = 10L;
        AiVendor vendor = AiVendor.builder().name("OpenAI").isActive(true).isDeprecated(false).build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT-5.4")
                .commonDescription("desc")
                .inputTypes(new String[] {"text"})
                .outputTypes(new String[] {"text"})
                .build();
        when(aiModelFamilyRepository.findAllChangedBetweenAndVendorId(
                        targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay(), vendorId))
                .thenReturn(List.of(family));

        List<ModelInfoFamilyView> result = infoCommunityReadService.getModelInfoByDateAndVendor(targetDate, vendorId);

        verify(aiModelFamilyRepository)
                .findAllChangedBetweenAndVendorId(targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay(), vendorId);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().vendorName()).isEqualTo("OpenAI");
    }

    @Test
    void getPerformanceMetricsByDate_returnsMappedResult() {
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        ModelBenchmark benchmark = ModelBenchmark.builder()
                .modelApiId("gpt-5.4")
                .metricType(MetricType.INTELLIGENCE)
                .metricValue(new BigDecimal("92.10"))
                .unit("score")
                .measuredAt(LocalDateTime.of(2026, 4, 8, 12, 0))
                .build();
        when(modelBenchmarkRepository.findAllByMeasuredAtBetween(
                        targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(benchmark));

        List<BenchmarkMetricView> result = infoCommunityReadService.getPerformanceMetricsByDate(targetDate);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().modelApiId()).isEqualTo("gpt-5.4");
        assertThat(result.getFirst().metricType()).isEqualTo(MetricType.INTELLIGENCE);
    }
}
