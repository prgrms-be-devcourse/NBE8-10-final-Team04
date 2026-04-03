package back.domain.info.service;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * OCI Object Storage에서 JSON 파일을 읽어 반환하는 컴포넌트.
 *
 * 읽기만 담당하고 처리(파싱·저장)는 호출자에게 위임한다.
 * AiInfoService를 직접 참조하지 않으므로 AiInfoServiceImpl과의 순환 참조가 없다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OciObjectStorageProcessor {

    private final ObjectProvider<ObjectStorage> objectStorageProvider;

    @Value("${oci.namespace}")
    private String namespace;

    @Value("${oci.bucket}")
    private String bucket;

    @Value("${app.ai-info.oci-prefix:}")
    private String promptsOciPrefix;

    /**
     * OCI 버킷에서 JSON 파일을 읽어 반환한다.
     */
    public String readFromOci(String basePath) {
        ObjectStorage objectStorage = objectStorageProvider.getIfAvailable();
        if (objectStorage == null) {
            log.error("OCI ObjectStorage 빈이 없어 JSON을 읽을 수 없습니다.");
            return null;
        }

        if (bucket == null || bucket.isBlank() || namespace == null || namespace.isBlank()) {
            log.warn("OCI 버킷 또는 네임스페이스가 설정되지 않아 실행을 건너뜁니다. bucket='{}', namespace='{}'",
                    bucket, namespace);
            return null;
        }

        try {
            String content = readObjectContent(objectStorage, basePath);
            return content;
        } catch (Exception e) {
            log.error("OCI 파일 읽기 실패: {}", basePath, e);
        }

        return null;
    }

    /**
     * OCI에서 단일 오브젝트의 내용을 UTF-8 문자열로 읽어 반환한다.
     */
    private String readObjectContent(ObjectStorage objectStorage, String objectName) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucket)
                .objectName(objectName)
                .build();

        GetObjectResponse response = objectStorage.getObject(request);
        try (InputStream inputStream = response.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * OCI prefix가 '/'로 끝나지 않으면 끝에 '/'를 붙여 반환한다.
     * OCI listObjects는 prefix가 정확히 일치해야 하위 오브젝트를 필터링할 수 있다.
     */
    private String normalizePrefix() {
        if (promptsOciPrefix == null || promptsOciPrefix.isBlank()) {
            return "";
        }
        return promptsOciPrefix.endsWith("/") ? promptsOciPrefix : promptsOciPrefix + "/";
    }
}
