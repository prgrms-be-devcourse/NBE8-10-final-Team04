package back.domain.info.service;

import back.domain.info.dto.data.ItemDto;
import back.domain.info.dto.response.PageUpdateRequestResponse;
import back.domain.info.dto.response.UpdateRequestResponse;
import back.domain.info.dto.data.UpdateRequestDto;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.entity.UpdateRequest;
import back.domain.info.enums.Status;
import back.domain.info.mapper.RequestMapper;
import back.domain.info.repository.AiModelFamilyRepository;
import back.domain.info.repository.AiVendorRepository;
import back.domain.info.repository.UpdateRequestRepository;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.storage.OciObjectStorageReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다."
)
public class UpdateRequestServiceImpl implements UpdateRequestService {

    private static final String BASE_PATH = "data/ai-tracker/updates_raw.json";

    private final ObjectMapper objectMapper;
    private final OciObjectStorageReader storageReader;
    private final UpdateRequestRepository requestRepository;
    private final AiVendorRepository aiVendorRepository;
    private final AiModelFamilyRepository familyRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public void run() {
        log.info("[UpdateRequestService#run] 시작. path={}", BASE_PATH);

        String content = storageReader.readText(BASE_PATH);
        if (content == null) return;

        UpdateRequestDto requestDto;
        try {
            requestDto = objectMapper.readValue(content, UpdateRequestDto.class);
        } catch (Exception e) {
            log.error("[UpdateRequestService#run] JSON 파싱 실패", e);
            return;
        }

        int success = 0, fail = 0;
        for (ItemDto dto : requestDto.items()) {
            try {
                processJson(dto);
                success++;
            } catch (Exception e) {
                log.error("[UpdateRequestService#run] 처리 실패 스킵. id={}", dto.itemId(), e);
                fail++;
            }
        }

        log.info("[UpdateRequestService#run] 완료. 성공: {}", success);
    }

    private void processJson(ItemDto dto) {
        if (requestRepository.existsBySourceIdAndNotifiedAt(dto.itemId(), LocalDate.now())) {
            log.info("[UpdateRequestService] 중복 스킵. sourceId={}", dto.itemId());
            return;
        }

        AiVendor vendor = aiVendorRepository.findByName(dto.provider()).orElse(null);
        if (vendor == null) {
            log.info("[UpdateRequestService] 없는 vendor 스킵. name={}", dto.provider());
            return;
        }

        createUpdateRequest(dto, vendor);
    }

    private void createUpdateRequest(ItemDto dto, AiVendor vendor) {

        AiModelFamily family = null;
        if (dto.family() != null && !dto.family().isBlank()) {
            family = familyRepository.findByFamilyName(dto.family()).orElse(null);
        }

        UpdateRequest updateRequest = requestMapper.toUpdateRequestEntity(dto, vendor, family);

        requestRepository.save(updateRequest);

    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        Status newStatus;
        try {
            newStatus = Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServiceException(
                    CommonErrorCode.BAD_REQUEST,
                    "[UpdateRequestService#updateStatus] invalid status: " + status,
                    "유효하지 않은 상태 값입니다. (PENDING, APPROVED, REJECTED)"
            );
        }

        UpdateRequest updateRequest = requestRepository.findById(id)
                .orElseThrow(() -> new ServiceException(
                        CommonErrorCode.NOT_FOUND,
                        "[UpdateRequestService#updateStatus] update request not found. id=" + id,
                        "해당 내용을 찾을 수 없습니다."
                ));

        updateRequest.review(newStatus); // reviewedAt도 함께 업데이트
    }

    @Override
    @Transactional(readOnly = true)
    public PageUpdateRequestResponse getUpdates(Pageable pageable) {
        return new PageUpdateRequestResponse(requestRepository.findAll(pageable).map(UpdateRequestResponse::new));
    }

    @Override
    @Transactional(readOnly = true)
    public PageUpdateRequestResponse getUpdatesApproved(Pageable pageable) {
        return new PageUpdateRequestResponse(
                requestRepository.findAllByStatus(Status.APPROVED, pageable).map(UpdateRequestResponse::new)
        );
    }
}
