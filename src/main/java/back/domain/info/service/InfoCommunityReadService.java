package back.domain.info.service;

import java.time.LocalDate;
import java.util.List;

import back.domain.info.dto.response.BenchmarkMetricView;
import back.domain.info.dto.response.ModelInfoFamilyView;

public interface InfoCommunityReadService {

    List<ModelInfoFamilyView> getModelInfoByDate(LocalDate targetDate);

    List<ModelInfoFamilyView> getModelInfoByDateAndVendor(LocalDate targetDate, Long vendorId);

    List<BenchmarkMetricView> getPerformanceMetricsByDate(LocalDate targetDate);
}
