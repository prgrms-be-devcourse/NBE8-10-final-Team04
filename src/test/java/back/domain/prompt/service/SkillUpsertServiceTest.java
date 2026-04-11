package back.domain.prompt.prompt.service;

import back.domain.prompt.prompt.dto.AgentDto;
import back.domain.prompt.prompt.dto.PromptRepoItem;
import back.domain.prompt.prompt.dto.RepositoryDto;
import back.domain.prompt.prompt.dto.SkillDto;
import back.domain.prompt.prompt.entity.Agent;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;
import back.domain.prompt.prompt.parser.SkillNormalizeParser;
import back.domain.prompt.prompt.repository.AgentRepository;
import back.domain.prompt.prompt.repository.RepositoryRepository;
import back.domain.prompt.prompt.repository.SkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillUpsertServiceTest {

    @InjectMocks
    private SkillUpsertServiceImpl skillNormalizeService;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private SkillNormalizeParser parser;

    @Test
    @DisplayName("upsertRepository saves a new repository")
    void normalizeRepository_savesNewRepository() {
        PromptRepoItem repoItem = promptRepoItem(
                100L,
                LocalDateTime.parse("2026-03-26T10:00:00"),
                15,
                4,
                "etag-new"
        );
        when(repositoryRepository.findByGithubId(100L)).thenReturn(Optional.empty());
        when(repositoryRepository.save(any(Repository.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Repository saved = skillNormalizeService.upsertRepository(repoItem);

        ArgumentCaptor<Repository> repositoryCaptor = ArgumentCaptor.forClass(Repository.class);
        verify(repositoryRepository).save(repositoryCaptor.capture());
        Repository captured = repositoryCaptor.getValue();
        assertThat(saved).isSameAs(captured);
        assertThat(captured.getGithubId()).isEqualTo(100L);
        assertThat(captured.getName()).isEqualTo("demo-repo");
        assertThat(captured.getSourceRepo()).isEqualTo("owner/repo");
        assertThat(captured.getSourceUri()).isEqualTo("https://example.com/owner/repo");
        assertThat(captured.getOwnerType()).isEqualTo(OwnerType.USER);
    }

    @Test
    @DisplayName("upsertRepository keeps existing repository when timestamp is unchanged")
    void normalizeRepository_keepsExistingRepositoryUnchanged() {
        Repository existing = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        PromptRepoItem repoItem = promptRepoItem(
                100L,
                LocalDateTime.parse("2026-03-26T10:00:00"),
                30,
                7,
                "etag-new"
        );
        when(repositoryRepository.findByGithubId(100L)).thenReturn(Optional.of(existing));

        Repository result = skillNormalizeService.upsertRepository(repoItem);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getStarCount()).isEqualTo(3);
        assertThat(existing.getForkCount()).isEqualTo(1);
        assertThat(existing.getEtag()).isEqualTo("etag-old");
        verify(repositoryRepository, never()).save(any(Repository.class));
    }

    @Test
    @DisplayName("upsertSkill saves a new skill when absent")
    void normalizeSkill_savesNewSkill() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        SkillDto skillDto = skillData("alpha", "skills/alpha.md", "alpha content", "alpha-hash");
        when(parser.extractTags("demo summary", "alpha content")).thenReturn(Set.of("spring", "java"));
        when(parser.extractCategory("demo summary", "alpha content")).thenReturn(Category.BACKEND);
        when(skillRepository.findByRepositoryIdAndName(1L, "alpha")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Skill saved = skillNormalizeService.upsertSkill(repository, skillDto);

        ArgumentCaptor<Skill> skillCaptor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(skillCaptor.capture());
        Skill captured = skillCaptor.getValue();
        assertThat(saved).isSameAs(captured);
        assertThat(captured.getRepository()).isSameAs(repository);
        assertThat(captured.getName()).isEqualTo("alpha");
        assertThat(captured.getContentMd()).isEqualTo("alpha content");
        assertThat(captured.getContentHash()).isEqualTo("alpha-hash");
        assertThat(captured.getFilePath()).isEqualTo("skills/alpha.md");
        assertThat(captured.getTagsJson()).containsExactlyInAnyOrder("spring", "java");
        assertThat(captured.getCategory()).isEqualTo(Category.BACKEND);
    }

    @Test
    @DisplayName("upsertSkill updates existing skill when hash changes")
    void normalizeSkill_updatesExistingSkill() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        Skill existing = Skill.builder()
                .repository(repository)
                .name("alpha")
                .contentMd("old content")
                .contentHash("old-hash")
                .filePath("skills/alpha.md")
                .category(Category.OTHER)
                .tagsJson(Set.of("legacy"))
                .build();
        SkillDto skillDto = skillData("alpha", "skills/alpha.md", "new content", "new-hash");
        when(parser.extractTags("demo summary", "new content")).thenReturn(Set.of("react", "typescript"));
        when(parser.extractCategory("demo summary", "new content")).thenReturn(Category.FRONTEND);
        when(skillRepository.findByRepositoryIdAndName(1L, "alpha")).thenReturn(Optional.of(existing));

        Skill result = skillNormalizeService.upsertSkill(repository, skillDto);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getContentMd()).isEqualTo("new content");
        assertThat(existing.getContentHash()).isEqualTo("new-hash");
        assertThat(existing.getTagsJson()).containsExactlyInAnyOrder("react", "typescript");
        assertThat(existing.getCategory()).isEqualTo(Category.FRONTEND);
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    @DisplayName("upsertAgent saves a new agent when absent")
    void normalizeAgent_savesNewAgent() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        AgentDto agentDto = agentData("codex", "AGENTS.md", "agent content", "agent-hash");
        when(agentRepository.findByRepositoryId(1L)).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Agent saved = skillNormalizeService.upsertAgent(repository, agentDto);

        ArgumentCaptor<Agent> agentCaptor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(agentCaptor.capture());
        Agent captured = agentCaptor.getValue();
        assertThat(saved).isSameAs(captured);
        assertThat(captured.getRepository()).isSameAs(repository);
        assertThat(captured.getContentMd()).isEqualTo("agent content");
        assertThat(captured.getContentHash()).isEqualTo("agent-hash");
        assertThat(captured.getFilePath()).isEqualTo("AGENTS.md");
    }

    @Test
    @DisplayName("upsertAgent updates existing agent when hash changes")
    void normalizeAgent_updatesExistingAgent() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        Agent existing = Agent.builder()
                .repository(repository)
                .contentMd("old agent content")
                .contentHash("old-agent-hash")
                .filePath("AGENTS.md")
                .build();
        AgentDto agentDto = agentData("codex", "AGENTS.md", "new agent content", "new-agent-hash");
        when(agentRepository.findByRepositoryId(1L)).thenReturn(Optional.of(existing));

        Agent result = skillNormalizeService.upsertAgent(repository, agentDto);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getContentMd()).isEqualTo("new agent content");
        assertThat(existing.getContentHash()).isEqualTo("new-agent-hash");
        verify(agentRepository, never()).save(any(Agent.class));
    }

    @Test
    @DisplayName("upsertSkills batch-saves new skills")
    void upsertSkills_savesNewSkillsInBatch() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        List<SkillDto> skillDtos = List.of(
                skillData("alpha", "skills/alpha.md", "alpha content", "alpha-hash"),
                skillData("beta", "skills/beta.md", "beta content", "beta-hash")
        );

        when(skillRepository.findByRepositoryId(1L)).thenReturn(List.of());
        when(skillRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(parser.extractTags("demo summary", "alpha content")).thenReturn(Set.of("spring"));
        when(parser.extractCategory("demo summary", "alpha content")).thenReturn(Category.BACKEND);
        when(parser.extractTags("demo summary", "beta content")).thenReturn(Set.of("java"));
        when(parser.extractCategory("demo summary", "beta content")).thenReturn(Category.BACKEND);

        skillNormalizeService.upsertSkills(repository, skillDtos);

        ArgumentCaptor<List> skillBatchCaptor = ArgumentCaptor.forClass(List.class);
        verify(skillRepository).saveAll(skillBatchCaptor.capture());
        assertThat(skillBatchCaptor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("upsertSkills continues with per-item fallback when batch save fails")
    void upsertSkills_fallbacksToSingleSaveWhenBatchFails() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );
        List<SkillDto> skillDtos = List.of(
                skillData("valid-skill", "skills/valid.md", "valid content", "valid-hash"),
                skillData(null, "skills/invalid.md", "invalid content", "invalid-hash")
        );

        when(skillRepository.findByRepositoryId(1L)).thenReturn(List.of());
        doThrow(new RuntimeException("batch failed")).when(skillRepository).saveAll(any());
        when(skillRepository.save(argThat(skill -> "valid-skill".equals(skill.getName()))))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("invalid row"))
                .when(skillRepository)
                .save(argThat(skill -> skill.getName() == null));
        when(parser.extractTags(anyString(), anyString())).thenReturn(Set.of("batch"));
        when(parser.extractCategory(anyString(), anyString())).thenReturn(Category.BACKEND);

        assertThatNoException().isThrownBy(() -> skillNormalizeService.upsertSkills(repository, skillDtos));

        verify(skillRepository).saveAll(any());
        verify(skillRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("upsertSkills는 신규 스킬을 200개 단위로 저장한다")
    void upsertSkills_splitsSkillsInto200Chunks() {
        Repository repository = repository(
                1L,
                100L,
                "owner/repo",
                3,
                1,
                "etag-old",
                LocalDateTime.parse("2026-03-26T10:00:00")
        );

        List<SkillDto> skillDtos = IntStream.range(0, 201)
                .mapToObj(i -> skillData("skill-" + i, "skills/skill-" + i + ".md", "content-" + i, "hash-" + i))
                .toList();

        when(skillRepository.findByRepositoryId(1L)).thenReturn(List.of());
        when(skillRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(parser.extractTags(anyString(), anyString())).thenReturn(Set.of("batch"));
        when(parser.extractCategory(anyString(), anyString())).thenReturn(Category.BACKEND);

        skillNormalizeService.upsertSkills(repository, skillDtos);

        ArgumentCaptor<List> skillBatchCaptor = ArgumentCaptor.forClass(List.class);
        verify(skillRepository, times(2)).saveAll(skillBatchCaptor.capture());
        assertThat(skillBatchCaptor.getAllValues().get(0)).hasSize(200);
        assertThat(skillBatchCaptor.getAllValues().get(1)).hasSize(1);
    }

    private PromptRepoItem promptRepoItem(
            Long githubId,
            LocalDateTime sourceUpdatedAt,
            Integer starCount,
            Integer forkCount,
            String etag
    ) {
        return new PromptRepoItem(
                repositoryData(githubId, sourceUpdatedAt, starCount, forkCount, etag),
                List.of(
                        skillData("alpha", "skills/alpha.md", "```java\\nSystem.out.println();\\n```", "alpha-hash"),
                        skillData("beta", "skills/beta.md", "```kotlin\\nprintln()\\n```", "beta-hash")
                ),
                null
        );
    }

    private RepositoryDto repositoryData(
            Long githubId,
            LocalDateTime sourceUpdatedAt,
            Integer starCount,
            Integer forkCount,
            String etag
    ) {
        return new RepositoryDto(
                githubId,
                "demo-repo",
                "owner/repo",
                "https://example.com/owner/repo",
                "demo summary",
                starCount,
                forkCount,
                50,
                null,
                "MIT",
                null,
                null,
                "user",
                true,
                "main",
                etag,
                sourceUpdatedAt,
                true,
                null
        );
    }

    private SkillDto skillData(String name, String filePath, String contentMd, String contentHash) {
        return new SkillDto(name, filePath, contentMd, contentHash, null);
    }

    private AgentDto agentData(String name, String filePath, String contentMd, String contentHash) {
        return new AgentDto(name, filePath, contentMd, contentHash, null);
    }

    private Repository repository(
            Long id,
            Long githubId,
            String sourceRepo,
            Integer starCount,
            Integer forkCount,
            String etag,
            LocalDateTime sourceUpdatedAt
    ) {
        Repository repository = Repository.builder()
                .githubId(githubId)
                .name("demo-repo")
                .sourceRepo(sourceRepo)
                .sourceUri("https://example.com/" + sourceRepo)
                .summary("demo summary")
                .starCount(starCount)
                .forkCount(forkCount)
                .size(50)
                .languageStats(Map.of("Java", 90, "Kotlin", 10))
                .license("MIT")
                .homepage("https://example.com")
                .ownerAvatarUrl("https://example.com/avatar.png")
                .ownerType(OwnerType.USER)
                .isOfficial(true)
                .defaultBranch("main")
                .etag(etag)
                .sourceUpdatedAt(sourceUpdatedAt)
                .active(true)
                .rawMetadata(Map.of("category", "demo"))
                .build();
        ReflectionTestUtils.setField(repository, "id", id);
        return repository;
    }
}
