package back.domain.info.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
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
import back.domain.info.dto.response.UpdateRequestCommunityView;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.entity.ModelBenchmark;
import back.domain.info.entity.UpdateRequest;
import back.domain.info.enums.MetricType;
import back.domain.info.enums.Status;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.ModelBenchmarkRepository;
import back.domain.info.repository.UpdateRequestRepository;

class InfoCommunityReadServiceImplTest {

    private AiModelFamilyRepository aiModelFamilyRepository;
    private ModelBenchmarkRepository modelBenchmarkRepository;
    private UpdateRequestRepository updateRequestRepository;
    private InfoCommunityReadService infoCommunityReadService;

    @BeforeEach
    void setUp() {
        aiModelFamilyRepository = mock(AiModelFamilyRepository.class);
        modelBenchmarkRepository = mock(ModelBenchmarkRepository.class);
        updateRequestRepository = mock(UpdateRequestRepository.class);
        infoCommunityReadService =
                new InfoCommunityReadServiceImpl(aiModelFamilyRepository, modelBenchmarkRepository, updateRequestRepository);
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

    @Test
    void getPendingUpdateRequestsByDateAndVendor_returnsMappedResult() {
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        Long vendorId = 10L;
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        AiVendor vendor = AiVendor.builder().name("OpenAI").isActive(true).isDeprecated(false).build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT-5.4")
                .commonDescription("desc")
                .build();
        UpdateRequest updateRequest = UpdateRequest.builder()
                .sourceId("source-1")
                .vendor(vendor)
                .family(family)
                .sourceUrl("https://news.example.com/openai")
                .sourceType("RSS")
                .summary("summary")
                .rawContent("raw")
                .status(Status.PENDING)
                .notifiedAt(targetDate.atTime(12, 0))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(updateRequest, "id", 101L);

        when(updateRequestRepository.findAllByStatusAndVendorIdAndNotifiedAtBetweenOrderByReviewedAtDescCreatedAtDesc(
                        Status.PENDING, vendorId, start, end))
                .thenReturn(List.of(updateRequest));

        List<UpdateRequestCommunityView> result =
                infoCommunityReadService.getPendingUpdateRequestsByDateAndVendor(targetDate, vendorId);

        verify(updateRequestRepository)
                .findAllByStatusAndVendorIdAndNotifiedAtBetweenOrderByReviewedAtDescCreatedAtDesc(
                        Status.PENDING, vendorId, start, end);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(101L);
        assertThat(result.getFirst().vendorName()).isEqualTo("OpenAI");
        assertThat(result.getFirst().summary()).isEqualTo("summary");
        assertThat(result.getFirst().rawContent()).isEqualTo("raw");
        assertThat(result.getFirst().notifiedAt()).isEqualTo(targetDate.atTime(12, 0));
    }

    @Test
    void approveUpdateRequests_updatesStatusToApproved() {
        UpdateRequest pendingOne = UpdateRequest.builder().sourceId("source-1").status(Status.PENDING).build();
        UpdateRequest pendingTwo = UpdateRequest.builder().sourceId("source-2").status(Status.PENDING).build();

        when(updateRequestRepository.findAllById(anySet())).thenReturn(List.of(pendingOne, pendingTwo));

        infoCommunityReadService.approveUpdateRequests(List.of(101L, 102L, 101L));

        verify(updateRequestRepository).findAllById(java.util.Set.of(101L, 102L));
        assertThat(pendingOne.getStatus()).isEqualTo(Status.APPROVED);
        assertThat(pendingTwo.getStatus()).isEqualTo(Status.APPROVED);
    }
}
