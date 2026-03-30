package back.domain.prompt.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;

import back.domain.prompt.dto.PromptRepoItem;
import back.domain.prompt.dto.SkillDto;
import back.domain.prompt.entity.Repository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "ObjectMapper와 ObjectStorage는 스프링 빈으로 관리한다.")
public class PromptServiceImpl implements PromptService {

    private static final String STORAGE_TYPE_OCI = "oci";

    private final SkillNormalizeService normalizeService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<ObjectStorage> objectStorageProvider;

    @Value("${app.storage.type:oci}")
    private String storageType;

    @Value("${app.prompts.base-path:data/prompts}")
    private String promptsBasePath;

    @Value("${app.storage.oci.namespace:}")
    private String namespace;

    @Value("${app.storage.oci.bucket:}")
    private String bucket;

    @Value("${app.prompts.oci-prefix:data/prompts/}")
    private String promptsOciPrefix;

    @Override
    public void run() {
        if (STORAGE_TYPE_OCI.equalsIgnoreCase(storageType)) {
            runFromOci();
            return;
        }

        runFromLocal();
    }

    private void runFromLocal() {
        File baseDir = new File(promptsBasePath);

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            log.error("프롬프트 디렉터리가 존재하지 않습니다: {}", baseDir.getAbsolutePath());
            return;
        }

        File[] jsonFiles = baseDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            log.warn("처리할 JSON 파일이 없습니다.");
            return;
        }

        Arrays.sort(jsonFiles, Comparator.comparing(File::getName));
        for (File jsonFile : jsonFiles) {
            try {
                processJson(jsonFile.getName(), Files.readString(jsonFile.toPath()));
            } catch (Exception e) {
                log.error("프롬프트 파일 처리 실패: {}", jsonFile.getName(), e);
            }
        }
    }

    private void runFromOci() {
        ObjectStorage objectStorage = objectStorageProvider.getIfAvailable();
        if (objectStorage == null) {
            log.error("OCI ObjectStorage 빈이 없어 프롬프트를 읽을 수 없습니다.");
            return;
        }

        List<String> objectNames = listJsonObjectNames(objectStorage);
        if (objectNames.isEmpty()) {
            log.warn("OCI Object Storage에서 처리할 JSON 파일이 없습니다. bucket={}, prefix={}", bucket, normalizePrefix());
            return;
        }

        for (String objectName : objectNames) {
            try {
                processJson(objectName, readObjectContent(objectStorage, objectName));
            } catch (Exception e) {
                log.error("프롬프트 파일 처리 실패: {}", objectName, e);
            }
        }
    }

    private List<String> listJsonObjectNames(ObjectStorage objectStorage) {
        List<String> objectNames = new ArrayList<>();
        String start = null;

        do {
            ListObjectsRequest request = ListObjectsRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucket)
                    .prefix(normalizePrefix())
                    .start(start)
                    .build();

            var response = objectStorage.listObjects(request);
            var objectList = response.getListObjects();

            for (ObjectSummary objectSummary : objectList.getObjects()) {
                if (objectSummary.getName() != null && objectSummary.getName().endsWith(".json")) {
                    objectNames.add(objectSummary.getName());
                }
            }

            start = objectList.getNextStartWith();
        } while (start != null);

        objectNames.sort(String::compareTo);
        return objectNames;
    }

    private String readObjectContent(ObjectStorage objectStorage, String objectName) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucket)
                .objectName(objectName)
                .build();

        var response = objectStorage.getObject(request);
        try (var inputStream = response.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String normalizePrefix() {
        if (promptsOciPrefix == null || promptsOciPrefix.isBlank()) {
            return "";
        }
        return promptsOciPrefix.endsWith("/") ? promptsOciPrefix : promptsOciPrefix + "/";
    }

    private void processJson(String resourceName, String json) {
        try {
            PromptRepoItem repoItem = objectMapper.readValue(json, PromptRepoItem.class);

            if (repoItem.getRepository() == null) {
                log.warn("repository 누락 스킵: {}", resourceName);
                return;
            }

            Repository repository = normalizeService.normalizeRepository(repoItem);

            if (repoItem.getSkills() != null && !repoItem.getSkills().isEmpty()) {
                for (SkillDto skillDto : repoItem.getSkills()) {
                    try {
                        normalizeService.normalizeSkill(repository, skillDto);
                    } catch (Exception e) {
                        log.error("Skill 처리 실패: {}/{}", repoItem.getRepository().getSourceRepo(), skillDto.getName(), e);
                    }
                }
            } else {
                log.warn("skills 없음: {}", repoItem.getRepository().getSourceRepo());
            }

            if (repoItem.getAgent() != null) {
                try {
                    normalizeService.normalizeAgent(repository, repoItem.getAgent());
                } catch (Exception e) {
                    log.error("Agent 처리 실패: {}", repoItem.getRepository().getSourceRepo(), e);
                }
            } else {
                log.warn("agent 없음: {}", repoItem.getRepository().getSourceRepo());
            }
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", resourceName, e);
        }
    }
}
