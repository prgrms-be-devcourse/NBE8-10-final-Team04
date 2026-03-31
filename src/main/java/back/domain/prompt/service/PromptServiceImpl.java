package back.domain.prompt.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import back.domain.prompt.dto.AgentDto;
import back.domain.prompt.entity.Agent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;

import java.io.InputStream;

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
        justification = "ObjectMapper와 ObjectStorage는 스프링 빈으로 관리한다."
)
public class PromptServiceImpl implements PromptService {

    private static final String STORAGE_TYPE_OCI = "oci";

    private final SkillNormalizeService normalizeService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<ObjectStorage> objectStorageProvider;

    @Value("${app.storage.type:}")
    private String storageType;

    @Value("${app.prompts.base-path:data/prompts}")
    private String promptsBasePath;

    @Value("${app.storage.oci.namespace:}")
    private String namespace;

    @Value("${app.storage.oci.bucket:}")
    private String bucket;

    @Value("${app.prompts.oci-prefix:data/prompts/}")
    private String promptsOciPrefix;

    /**
     * app.storage.type 설정값에 따라 OCI 또는 로컬 파일시스템에서 프롬프트를 읽어 처리한다.
     * application.yml 예시:
     *   app.storage.type: oci   → OCI Object Storage 사용
     *   app.storage.type: local → 로컬 디렉터리 사용 (기본값)
     */
    @Override
    public void run() {
        if (STORAGE_TYPE_OCI.equalsIgnoreCase(storageType)) {
            runFromOci();
            return;
        }

        runFromLocal();
    }

    /**
     * 로컬 파일시스템에서 JSON 프롬프트 파일을 읽어 처리한다.
     * app.prompts.base-path 경로 아래의 .json 파일을 파일명 순으로 처리한다.
     */
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

    /**
     * OCI Object Storage에서 JSON 프롬프트 파일을 읽어 처리한다.
     * ObjectStorage 빈이 없으면 (OCI 설정 누락) 즉시 종료한다.
     */
    private void runFromOci() {
        ObjectStorage objectStorage = objectStorageProvider.getIfAvailable();
        if (objectStorage == null) {
            log.error("OCI ObjectStorage 빈이 없어 프롬프트를 읽을 수 없습니다.");
            return;
        }

        if (bucket == null || bucket.isBlank() || namespace == null || namespace.isBlank()) {
            log.warn("OCI 버킷 또는 네임스페이스가 설정되지 않아 프롬프트 실행을 건너뜁니다. bucket='{}', namespace='{}'",
                    bucket, namespace);
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

    /**
     * OCI 버킷에서 .json 파일 목록을 페이지 단위로 전부 수집해 반환한다.
     * OCI listObjects API는 한 번에 최대 1000개를 반환하므로,
     * nextStartWith가 null이 될 때까지 반복해 전체 목록을 가져온다.
     */
    private List<String> listJsonObjectNames(ObjectStorage objectStorage) {
        List<String> objectNames = new ArrayList<>();
        String start = null;

        while (true) {
            var objectList = objectStorage.listObjects(
                    ListObjectsRequest.builder()
                            .namespaceName(namespace)
                            .bucketName(bucket)
                            .prefix(normalizePrefix())
                            .start(start)
                            .build()
            ).getListObjects();

            objectList.getObjects().stream()
                    .map(ObjectSummary::getName)
                    .filter(name -> name != null && name.endsWith(".json"))
                    .forEach(objectNames::add);

            // 다음 페이지가 없으면 종료
            start = objectList.getNextStartWith();
            if (start == null) {
                break;
            }
        }

        objectNames.sort(Comparator.naturalOrder());
        return objectNames;
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

    /**
     * JSON 문자열을 PromptRepoItem으로 파싱한 뒤 Repository, Skill, Agent 순서로 정규화한다.
     * Skill/Agent는 각각 독립적으로 처리하므로 하나가 실패해도 나머지는 계속 진행된다.
     */
    private void processJson(String resourceName, String json) {
        try {
            PromptRepoItem repoItem = objectMapper.readValue(json, PromptRepoItem.class);

            if (repoItem.getRepository() == null) {
                log.warn("repository 누락 스킵: {}", resourceName);
                return;
            }

            Repository repository = normalizeService.normalizeRepository(repoItem);
            String sourceRepo = repository.getSourceRepo();

            processSkills(repository, sourceRepo, repoItem.getSkills());
            processAgent(repository, sourceRepo, repoItem.getAgent());

        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", resourceName, e);
        }
    }

    private void processSkills(Repository repository, String sourceRepo, List<SkillDto> skills) {
        if (skills == null || skills.isEmpty()) {
            log.warn("skills 없음: {}", sourceRepo);
            return;
        }

        for (SkillDto skillDto : skills) {
            if (skillDto.getContentMd() == null) {
                log.warn("skill content_md 없음: {}/{}", sourceRepo, skillDto.getName());
                continue;
            }

            try {
                normalizeService.normalizeSkill(repository, skillDto);
            } catch (Exception e) {
                log.error("Skill 처리 실패: {}/{}", sourceRepo, skillDto.getName(), e);
            }
        }
    }

    private void processAgent(Repository repository, String sourceRepo, AgentDto agent) {
        if (agent == null) {
            log.warn("agent 없음: {}", sourceRepo);
            return;
        }

        if (agent.getContentMd() == null) {
            log.warn("agent content_md 없음: {}", sourceRepo);
            return;
        }

        try {
            normalizeService.normalizeAgent(repository, agent);
        } catch (Exception e) {
            log.error("Agent 처리 실패: {}", sourceRepo, e);
        }
    }
}
