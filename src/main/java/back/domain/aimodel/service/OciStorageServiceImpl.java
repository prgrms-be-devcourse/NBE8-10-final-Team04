package back.domain.aimodel.service;

import back.domain.aimodel.config.OciProperties;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "oci")
public class OciStorageServiceImpl implements OciStorageService {

    private final OciProperties props;
    private final ObjectMapper  objectMapper;
    private final ObjectStorage client;

    // ── 다운로드 ──────────────────────────────────────────────────────────────

    @Override
    public byte[] download(String objectName) {
        log.info("[OciStorageService#download] OCI 다운로드: {}", objectName);
        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(props.namespace())
                .bucketName(props.bucket())
                .objectName(objectName)
                .build();

        try {
            GetObjectResponse response = client.getObject(request);
            try (InputStream is = response.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#download] OCI 다운로드 실패 - objectName: " + objectName,
                    "OCI 스토리지에서 파일을 다운로드하는데 실패했습니다."
            );
        }
    }

    @Override
    public <T> T downloadJson(String objectName, Class<T> clazz) {
        byte[] bytes = download(objectName);
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#downloadJson] JSON 역직렬화 실패 - objectName: " + objectName,
                    "JSON 데이터를 객체로 변환하는데 실패했습니다."
            );
        }
    }

    // ── 업로드 ────────────────────────────────────────────────────────────────

    @Override
    public void upload(String objectName, byte[] content, String contentType) {
        log.info("[OciStorageService#upload] OCI 업로드: {} ({} bytes)", objectName, content.length);
        PutObjectRequest request = PutObjectRequest.builder()
                .namespaceName(props.namespace())
                .bucketName(props.bucket())
                .objectName(objectName)
                .putObjectBody(new ByteArrayInputStream(content))
                .contentLength((long) content.length)
                .contentType(contentType)
                .build();

        try {
            client.putObject(request);
            log.info("[OciStorageService#upload] OCI 업로드 완료: {}", objectName);
        } catch (Exception e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#upload] OCI 업로드 실패 - objectName: " + objectName,
                    "OCI 스토리지에 파일을 업로드하는데 실패했습니다."
            );
        }
    }

    @Override
    public void uploadJson(String objectName, Object data) {
        try {
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(data)
                    .getBytes(StandardCharsets.UTF_8);
            upload(objectName, bytes, "application/json");
        } catch (IOException e) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[OciStorageService#uploadJson] JSON 직렬화 실패 - objectName: " + objectName,
                    "데이터를 JSON 형식으로 변환하는데 실패했습니다."
            );
        }
    }

    // ── 편의 메서드 ───────────────────────────────────────────────────────────

    @Override
    public String objectName(String filename) {
        return props.prefix() + filename;
    }
}