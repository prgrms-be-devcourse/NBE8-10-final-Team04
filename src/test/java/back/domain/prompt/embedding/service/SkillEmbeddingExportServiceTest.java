package back.domain.prompt.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.prompt.embedding.dto.SkillEmbeddingExportDto;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;
import back.domain.prompt.prompt.repository.SkillRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SkillEmbeddingExportServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private SkillRepository skillRepository;

    private SkillEmbeddingExportService skillEmbeddingExportService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        skillEmbeddingExportService = new SkillEmbeddingExportService(skillRepository, objectMapper);
        ReflectionTestUtils.setField(skillEmbeddingExportService, "exportDir", tempDir.toString());
    }

    @Test
    @DisplayName("exportAllSkillsToJson은 파일로 내보내고 경로를 반환한다")
    void exportAllSkillsToJson_writesJsonFile() throws IOException {
        when(skillRepository.findAllByOrderByIdAsc()).thenReturn(List.of(skill(1L, repository(10L), "alpha")));

        Path exported = skillEmbeddingExportService.exportAllSkillsToJson();

        assertThat(exported).exists();

        JsonNode root = objectMapper.readTree(exported.toFile());
        assertThat(root).hasSize(1);
        assertThat(root.get(0).get("skill_id").asLong()).isEqualTo(1L);
        assertThat(root.get(0).get("repository_id").asLong()).isEqualTo(10L);
        assertThat(root.get(0).get("name").asText()).isEqualTo("alpha");
    }

    @Test
    @DisplayName("getAllSkillsForExport는 repository가 없어도 DTO로 변환한다")
    void getAllSkillsForExport_mapsSkillWithoutRepository() {
        when(skillRepository.findAllByOrderByIdAsc()).thenReturn(List.of(skill(2L, null, "beta")));

        List<SkillEmbeddingExportDto> result = skillEmbeddingExportService.getAllSkillsForExport();

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getSkillId()).isEqualTo(2L);
            assertThat(dto.getRepositoryId()).isNull();
            assertThat(dto.getName()).isEqualTo("beta");
            assertThat(dto.getPath()).isEqualTo("skills/beta.md");
        });
    }

    @Test
    @DisplayName("exportAllSkillsToJson은 디렉터리 생성에 실패하면 예외를 던진다")
    void exportAllSkillsToJson_throwsWhenDirectoryCannotBeCreated() throws IOException {
        Path filePath = tempDir.resolve("not-a-directory");
        Files.writeString(filePath, "content");
        ReflectionTestUtils.setField(skillEmbeddingExportService, "exportDir", filePath.toString());
        when(skillRepository.findAllByOrderByIdAsc()).thenReturn(List.of(skill(1L, repository(10L), "alpha")));

        assertThatThrownBy(() -> skillEmbeddingExportService.exportAllSkillsToJson())
                .isInstanceOf(IllegalStateException.class);
    }

    private Skill skill(Long id, Repository repository, String name) {
        Skill skill = Skill.builder()
                .repository(repository)
                .name(name)
                .contentMd(name + " content")
                .contentHash(name + "-hash")
                .filePath("skills/" + name + ".md")
                .category(Category.BACKEND)
                .build();
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }

    private Repository repository(Long id) {
        Repository repository = Repository.builder()
                .githubId(100L)
                .name("demo-repo")
                .sourceRepo("owner/repo")
                .sourceUri("https://example.com/owner/repo")
                .summary("demo summary")
                .starCount(10)
                .forkCount(2)
                .size(100)
                .languageStats(Map.of("Java", 100))
                .license("MIT")
                .homepage("https://example.com")
                .ownerAvatarUrl("https://example.com/avatar.png")
                .ownerType(OwnerType.USER)
                .isOfficial(true)
                .defaultBranch("main")
                .etag("etag")
                .sourceUpdatedAt(LocalDateTime.parse("2026-03-31T00:00:00"))
                .active(true)
                .build();
        ReflectionTestUtils.setField(repository, "id", id);
        return repository;
    }
}
