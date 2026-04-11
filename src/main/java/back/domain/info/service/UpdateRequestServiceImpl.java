package back.domain.info.service;

import back.domain.info.dto.data.ItemDto;
import back.domain.info.dto.data.UpdateRequestDto;
import back.domain.info.dto.response.PageUpdateRequestResponse;
import back.domain.info.dto.response.UpdateRequestResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring-managed ObjectMapper is injected and used only within this service."
)
public class UpdateRequestServiceImpl implements UpdateRequestService {

    private static final String BASE_PATH = "data/ai-tracker/updates.json";

    private final ObjectMapper objectMapper;
    private final OciObjectStorageReader storageReader;
    private final UpdateRequestRepository requestRepository;
    private final AiVendorRepository aiVendorRepository;
    private final AiModelFamilyRepository familyRepository;
    private final RequestMapper requestMapper;

    @Override
    @Transactional
    public void getUpdateRequest() {
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

        int success = 0;
        int fail = 0;
        for (ItemDto dto : requestDto.items()) {
            try {
                processJson(dto);
                success++;
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("[UpdateRequestService#run] 처리 실패 스킵. id={}", dto.sourceId(), e);
                fail++;
            }
        }

        log.info("[UpdateRequestService#run] 완료. success={}, fail={}", success, fail);
    }

    private void processJson(ItemDto dto) {
        String provider = null;
        if(dto.provider().equals("google")) {
            provider = "Google";
        }
        else if (dto.provider().equals("openai")) {
            provider = "OpenAI";
        }
        else if (dto.provider().equals("anthropic")) {
            provider = "Anthropic";
        }

        AiVendor vendor = aiVendorRepository.findByName(provider)
                .orElseThrow(() -> new ServiceException(
                        CommonErrorCode.NOT_FOUND,
                        "[UpdateRequestService#processJson] update request 생성 실패. sourceId="
                                + dto.sourceId() + ", provider=" + dto.provider(),
                        "Vendor를 찾을 수 없습니다."
                ));

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
                    "[UpdateRequestService#updateStatus] Invalid Status : " + status,
                    "유효한 상태값이 아닙니다. (PENDING, APPROVED, REJECTED)"
            );
        }

        UpdateRequest updateRequest = requestRepository.findById(id)
                .orElseThrow(() -> new ServiceException(
                        CommonErrorCode.NOT_FOUND,
                        "[UpdateRequestService#updateStatus] update request not found. id=" + id,
                        "Update request를 찾을 수 없습니다."
                ));

        updateRequest.review(newStatus);
    }

    @Override
    @Transactional(readOnly = true) // Entity에서 LAZY로 지정, 조회 + DTO 매핑이 끝날 때까지 영속성 컨텍스트를 유지
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
