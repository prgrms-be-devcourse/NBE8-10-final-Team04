package back.domain.prompt.prompt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.model.ListObjects;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;

import back.domain.prompt.prompt.dto.SkillDto;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.enums.OwnerType;
import tools.jackson.databind.ObjectMapper;

class PromptServiceImplTest {

    @TempDir
    Path tempDir;

    private SkillUpsertService normalizeService;
    private ObjectProvider<ObjectStorage> objectStorageProvider;
    private PromptServiceImpl promptServiceImpl;

    @BeforeEach
    void setUp() {
        normalizeService = mock(SkillUpsertService.class);
        objectStorageProvider = mock(ObjectProvider.class);
        promptServiceImpl = new PromptServiceImpl(normalizeService, new ObjectMapper(), objectStorageProvider);
    }

    @Test
    @DisplayName("run은 로컬 json 파일을 처리해 repository, skills, agent를 정규화한다")
    void run_processesLocalJsonFiles() throws IOException {
        writeFile("prompt.json", validPromptJson());
        Repository repository = repository(1L, "owner/repo");
        when(normalizeService.upsertRepository(any())).thenReturn(repository);
        setLocalStorage(tempDir);

        promptServiceImpl.run();

        verifyNormalized(repository);
    }

    @Test
    @DisplayName("run은 OCI json 객체를 처리해 repository, skills, agent를 정규화한다")
    void run_processesOciJsonFiles() {
        ObjectStorage objectStorage = mock(ObjectStorage.class);
        ListObjectsResponse listObjectsResponse = mock(ListObjectsResponse.class);
        ListObjects listObjects = mock(ListObjects.class);
        GetObjectResponse getObjectResponse = mock(GetObjectResponse.class);
        Repository repository = repository(1L, "owner/repo");

        when(objectStorageProvider.getIfAvailable()).thenReturn(objectStorage);
        when(objectStorage.listObjects(any())).thenReturn(listObjectsResponse);
        when(listObjectsResponse.getListObjects()).thenReturn(listObjects);
        when(listObjects.getObjects()).thenReturn(
                List.of(ObjectSummary.builder().name("data/prompts/prompt.json").build())
        );
        when(listObjects.getNextStartWith()).thenReturn(null);
        when(objectStorage.getObject(any())).thenReturn(getObjectResponse);
        when(getObjectResponse.getInputStream()).thenReturn(
                new ByteArrayInputStream(validPromptJson().getBytes(StandardCharsets.UTF_8))
        );
        when(normalizeService.upsertRepository(any())).thenReturn(repository);
        setOciStorage();

        promptServiceImpl.run();

        verifyNormalized(repository);
    }

    @Test
    @DisplayName("run은 repository payload가 없으면 스킵한다")
    void run_skipsFileWithoutRepository() throws IOException {
        writeFile("prompt.json", missingRepositoryJson());
        setLocalStorage(tempDir);

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    @Test
    @DisplayName("run은 skill 하나 정규화에 실패해도 나머지 skills와 agent 처리를 계속한다")
    void run_continuesWhenSkillNormalizationFails() throws IOException {
        writeFile("prompt.json", validPromptJson());
        Repository repository = repository(1L, "owner/repo");
        when(normalizeService.upsertRepository(any())).thenReturn(repository);
        doThrow(new IllegalStateException("boom"))
                .when(normalizeService)
                .upsertSkill(same(repository), argThat(skill -> "alpha".equals(skill.getName())));
        setLocalStorage(tempDir);

        assertThatNoException().isThrownBy(() -> promptServiceImpl.run());

        verify(normalizeService).upsertSkill(
                same(repository),
                argThat(skill -> "alpha".equals(skill.getName()))
        );
        verify(normalizeService).upsertSkill(
                same(repository),
                argThat(skill -> "beta".equals(skill.getName()))
        );
        verify(normalizeService).upsertAgent(
                same(repository),
                argThat(agent -> "agent-hash".equals(agent.getContentHash()))
        );
    }

    @Test
    @DisplayName("run은 잘못된 json 파일을 무시한다")
    void run_ignoresInvalidJson() throws IOException {
        writeFile("broken.json", "{ not-valid-json");
        setLocalStorage(tempDir);

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    @Test
    @DisplayName("run은 로컬 프롬프트 디렉터리가 없으면 종료한다")
    void run_returnsWhenPromptDirectoryMissing() {
        setLocalStorage(tempDir.resolve("missing"));

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    @Test
    @DisplayName("run은 로컬 디렉터리에 json 파일이 없으면 종료한다")
    void run_returnsWhenNoJsonFilesExist() throws IOException {
        writeFile("notes.txt", "plain text");
        setLocalStorage(tempDir);

        promptServiceImpl.run();

        verifyNoInteractions(normalizeService);
    }

    private void verifyNormalized(Repository repository) {
        verify(normalizeService).upsertRepository(
                argThat(item -> item.getRepository() != null
                        && "owner/repo".equals(item.getRepository().getSourceRepo()))
        );

        ArgumentCaptor<SkillDto> skillCaptor = ArgumentCaptor.forClass(SkillDto.class);
        verify(normalizeService, times(2)).upsertSkill(same(repository), skillCaptor.capture());
        assertThat(skillCaptor.getAllValues())
                .extracting(SkillDto::getName)
                .containsExactly("alpha", "beta");

        verify(normalizeService).upsertAgent(
                same(repository),
                argThat(agent -> agent != null && "agent-hash".equals(agent.getContentHash()))
        );
    }

    private void setLocalStorage(Path path) {
        ReflectionTestUtils.setField(promptServiceImpl, "storageType", "local");
        ReflectionTestUtils.setField(promptServiceImpl, "promptsBasePath", path.toString());
    }

    private void setOciStorage() {
        ReflectionTestUtils.setField(promptServiceImpl, "storageType", "oci");
        ReflectionTestUtils.setField(promptServiceImpl, "namespace", "ns");
        ReflectionTestUtils.setField(promptServiceImpl, "bucket", "bucket");
        ReflectionTestUtils.setField(promptServiceImpl, "promptsOciPrefix", "data/prompts/");
    }

    private void writeFile(String fileName, String content) throws IOException {
        Files.writeString(tempDir.resolve(fileName), content);
    }

    private Repository repository(Long id, String sourceRepo) {
        Repository repository = Repository.builder()
                .githubId(100L)
                .name("demo-repo")
                .sourceRepo(sourceRepo)
                .sourceUri("https://example.com/" + sourceRepo)
                .summary("demo summary")
                .starCount(10)
                .forkCount(3)
                .size(50)
                .languageStats(Map.of("Java", 90))
                .license("MIT")
                .homepage("https://example.com")
                .ownerAvatarUrl("https://example.com/avatar.png")
                .ownerType(OwnerType.USER)
                .isOfficial(true)
                .defaultBranch("main")
                .etag("etag-1")
                .sourceUpdatedAt(LocalDateTime.parse("2026-03-26T00:00:00"))
                .active(true)
                .rawMetadata(Map.of("category", "demo"))
                .build();
        ReflectionTestUtils.setField(repository, "id", id);
        return repository;
    }

    private String validPromptJson() {
        return """
                {
                  "repository": {
                    "github_id": 100,
                    "name": "demo-repo",
                    "source_repo": "owner/repo",
                    "source_url": "https://example.com/owner/repo",
                    "summary": "demo summary",
                    "star_count": 10,
                    "fork_count": 3,
                    "size": 50,
                    "language_stats": {
                      "Java": 90
                    },
                    "license": "MIT",
                    "homepage": "https://example.com",
                    "owner_avatar_url": "https://example.com/avatar.png",
                    "owner_type": "user",
                    "is_official": true,
                    "default_branch": "main",
                    "etag": "etag-1",
                    "source_updated_at": "2026-03-26T00:00:00",
                    "active": true,
                    "raw_metadata": {
                      "category": "demo"
                    }
                  },
                  "skills": [
                    {
                      "skill_name": "alpha",
                      "file_path": "skills/alpha.md",
                      "content_md": "alpha content",
                      "content_hash": "alpha-hash"
                    },
                    {
                      "skill_name": "beta",
                      "file_path": "skills/beta.md",
                      "content_md": "beta content",
                      "content_hash": "beta-hash"
                    }
                  ],
                  "agent": {
                    "name": "codex",
                    "file_path": "AGENTS.md",
                    "content_md": "agent content",
                    "content_hash": "agent-hash"
                  }
                }
                """;
    }

    private String missingRepositoryJson() {
        return """
                {
                  "repository": null,
                  "skills": [
                    {
                      "skill_name": "alpha",
                      "file_path": "skills/alpha.md",
                      "content_md": "alpha content",
                      "content_hash": "alpha-hash"
                    }
                  ],
                  "agent": {
                    "name": "codex",
                    "file_path": "AGENTS.md",
                    "content_md": "agent content",
                    "content_hash": "agent-hash"
                  }
                }
                """;
    }
}
