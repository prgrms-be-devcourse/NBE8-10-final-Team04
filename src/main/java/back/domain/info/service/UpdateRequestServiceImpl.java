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

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링이 관리하는 ObjectMapper를 DI로 주입받아 서비스 내부에서만 사용한다."
)
public class UpdateRequestServiceImpl implements UpdateRequestService {

    private static final String INVALID_STATUS = "유효하지 않는 상태 값 입니다. (PENDING, APPROVED, REJECTED)";

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
        String content = storageReader.readText(BASE_PATH);
        if (content == null) return;

        UpdateRequestDto requestDto;
        try {
            requestDto = objectMapper.readValue(content, UpdateRequestDto.class);
        } catch (Exception e) {
            log.error("[UpdateRequestServiceImpl#run] JSON 파싱 실패", e);
            return;
        }

        int success = 0, fail = 0;
        for(ItemDto dto : requestDto.items()) {
            try {
                processJson(dto);
                success++;
            } catch (Exception e) {
                log.error("[UpdateRequestServiceImpl#run] 아이템 업데이트 실패", e);
                fail++;
            }
        }

        log.info("[UpdateRequestServiceImpl#run] 완료. 성공: {}, 실패: {}", success, fail);
    }

    private void processJson(ItemDto dto) {

        AiVendor vendor = aiVendorRepository.findByName(dto.provider()).orElse(null);

        if (vendor == null) {
            log.warn("[UpdateRequestServiceImpl#processJson] 없는 vendor입니다.");
            return;
        }

        createUpdateRequest(dto, vendor);
    }

    private void createUpdateRequest(ItemDto dto, AiVendor vendor) {

        AiModelFamily family = familyRepository.findByFamilyName(dto.family()).orElse(null);

        UpdateRequest updateRequest = requestMapper.toUpdateRequestEntity(dto, vendor, family);

        requestRepository.save(updateRequest);

    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {

        String newStatus = status.toUpperCase();

        // Status enum 값이 유효한지 검증
        if (isValidStatus(newStatus)) {
            UpdateRequest updateRequest = requestRepository.findById(id).orElse(null);
            if (updateRequest != null) {
                updateRequest.setStatus(Status.valueOf(newStatus));
            }
        } else {
            // 유효하지 않은 상태일 경우
            throw new ServiceException(
                    CommonErrorCode.BAD_REQUEST,
                    "[RequestService#updateStatus] invalid status value",
                    INVALID_STATUS
            );
        }
    }

    // Status enum에 해당하는 값인지 체크하는 메서드
    private boolean isValidStatus(String status) {
        try {
            Status.valueOf(status); // status가 Status enum에 있는지 확인
            return true;
        } catch (IllegalArgumentException e) {
            return false; // enum에 없으면 false 반환
        }
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
