package back.domain.info.service;

import java.time.LocalDate;
import java.util.List;

import back.domain.info.dto.response.BenchmarkMetricView;
import back.domain.info.dto.response.ModelInfoFamilyView;
import back.domain.info.dto.response.UpdateRequestCommunityView;

public interface InfoCommunityReadService {

    List<ModelInfoFamilyView> getModelInfoByDate(LocalDate targetDate);

    List<ModelInfoFamilyView> getModelInfoByDateAndVendor(LocalDate targetDate, Long vendorId);

    List<UpdateRequestCommunityView> getPendingUpdateRequestsByDateAndVendor(LocalDate targetDate, Long vendorId);

    void approveUpdateRequests(List<Long> updateRequestIds);

    List<BenchmarkMetricView> getPerformanceMetricsByDate(LocalDate targetDate);
}