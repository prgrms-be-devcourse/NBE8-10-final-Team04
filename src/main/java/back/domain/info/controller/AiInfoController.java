package back.domain.info.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import back.domain.info.dto.response.FamilyDetailResponse;
import back.domain.info.dto.response.FamilySummaryResponse;
import back.domain.info.dto.response.PageUpdateRequestResponse;
import back.domain.info.dto.response.VendorInfoResponse;
import back.domain.info.service.AiInfoService;
import back.domain.info.service.InfoCatalogQueryService;
import back.domain.info.service.ModelBenchmarkService;
import back.domain.info.service.UpdateRequestService;
import back.global.response.RsData;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/info")
public class AiInfoController {
    private final AiInfoService inforService;
    private final ModelBenchmarkService benchmarkService;
    private final UpdateRequestService updateRequestService;
    private final InfoCatalogQueryService infoCatalogQueryService;

    @PostMapping("/model")
    public ResponseEntity<RsData<Void>> getModel() {
        inforService.getAiInfo();

        return ResponseEntity.ok(new RsData<>("데이터 적재 완료"));
    }

    @PostMapping("/benchmark")
    public ResponseEntity<RsData<Void>> getBenchmark() {
        benchmarkService.getModelBenchmark();

        return ResponseEntity.ok(new RsData<>("데이터 적재 완료"));
    }

    // Object Storage update_raw.json에서 가져온 데이터 적재
    @PostMapping("/update")
    public ResponseEntity<RsData<Void>> getUpdateRequests() {
        updateRequestService.getUpdateRequest();

        return ResponseEntity.ok(new RsData<>("데이터 적재 완료"));
    }

    // PENDING 상태 update들 상태 업데이트 (PENDING -> APPROVED / REJECTED)
    @PutMapping("/update")
    public ResponseEntity<RsData<Void>> updateStatus(@RequestParam Long id, @RequestParam String status) {

        updateRequestService.updateStatus(id, status);

        return ResponseEntity.ok(new RsData<>("상태 업데이트 완료"));
    }

    // 승인된 update 게시글만 - 전체 이용 가능  => 일단 전체 페이지네이션
    @GetMapping("/update/approved")
    public PageUpdateRequestResponse getUpdateApproved(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return updateRequestService.getUpdatesApproved(pageable);
    }

    // 승인 전 전체 update 게시글 - 관리자만 이용 가능
    @GetMapping("/update")
    public PageUpdateRequestResponse getUpdate(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return updateRequestService.getUpdates(pageable);
    }

    // 벤더 목록 조회 (공개)
    @GetMapping("/vendors")
    public ResponseEntity<RsData<List<VendorInfoResponse>>> getVendors() {
        return ResponseEntity.ok(new RsData<>(infoCatalogQueryService.getVendors(), "벤더 목록 조회 성공"));
    }

    // 특정 벤더의 패밀리 목록 조회 (공개)
    @GetMapping("/vendors/{vendorId}/families")
    public ResponseEntity<RsData<List<FamilySummaryResponse>>> getFamiliesByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(new RsData<>(infoCatalogQueryService.getFamiliesByVendorId(vendorId), "패밀리 목록 조회 성공"));
    }

    // 패밀리 상세 조회 (공개)
    @GetMapping("/families/{familyId}")
    public ResponseEntity<RsData<FamilyDetailResponse>> getFamilyDetail(@PathVariable Long familyId) {
        return ResponseEntity.ok(new RsData<>(infoCatalogQueryService.getFamilyDetail(familyId), "패밀리 상세 조회 성공"));
    }
}
