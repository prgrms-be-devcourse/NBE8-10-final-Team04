package back.domain.info.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import back.domain.info.dto.response.BenchmarkMetricView;
import back.domain.info.dto.response.ModelInfoFamilyView;
import back.domain.info.dto.response.UpdateRequestCommunityView;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.ModelBenchmarkRepository;
import back.domain.info.repository.UpdateRequestRepository;
import back.domain.info.enums.Status;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "스프링 DI로 주입되는 빈 참조이며 의도된 패턴이다.")
@Transactional(readOnly = true)
public class InfoCommunityReadServiceImpl implements InfoCommunityReadService {

    private final AiModelFamilyRepository aiModelFamilyRepository;
    private final ModelBenchmarkRepository modelBenchmarkRepository;
    private final UpdateRequestRepository updateRequestRepository;

    @Override
    public List<ModelInfoFamilyView> getModelInfoByDate(LocalDate targetDate) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        return aiModelFamilyRepository.findAllChangedBetween(start, end).stream()
                .map(ModelInfoFamilyView::from)
                .toList();
    }

    @Override
    public List<ModelInfoFamilyView> getModelInfoByDateAndVendor(LocalDate targetDate, Long vendorId) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        return aiModelFamilyRepository.findAllChangedBetweenAndVendorId(start, end, vendorId).stream()
                .map(ModelInfoFamilyView::from)
                .toList();
    }

    @Override
    public List<UpdateRequestCommunityView> getApprovedUpdateRequestsByDateAndVendor(LocalDate targetDate, Long vendorId) {
        return updateRequestRepository.findAllByStatusAndVendorIdAndNotifiedAtOrderByReviewedAtDescCreatedAtDesc(
                        Status.APPROVED, vendorId, targetDate).stream()
                .map(UpdateRequestCommunityView::from)
                .toList();
    }

    @Override
    public List<BenchmarkMetricView> getPerformanceMetricsByDate(LocalDate targetDate) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        return modelBenchmarkRepository.findAllByMeasuredAtBetween(start, end).stream()
                .map(BenchmarkMetricView::from)
                .toList();
    }
}
