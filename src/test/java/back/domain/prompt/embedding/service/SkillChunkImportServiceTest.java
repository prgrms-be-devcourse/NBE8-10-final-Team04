package back.domain.prompt.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.prompt.embedding.dto.SkillChunkImportDto;
import back.domain.prompt.embedding.entity.SkillChunk;
import back.domain.prompt.embedding.repository.SkillChunkRepository;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;
import back.domain.prompt.prompt.repository.SkillRepository;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SkillChunkImportServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillChunkRepository skillChunkRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("importFromJsonl은 chunk를 skill별로 교체 저장한다")
    @SuppressWarnings("unchecked")
    void importFromJsonl_replacesChunksBySkill() throws IOException {
        SkillChunkImportService service =
                new SkillChunkImportService(skillRepository, skillChunkRepository, objectMapper);
        Skill skill = skill(1L);
        SkillChunkImportDto second = chunkDto(1L, 2, "Install", List.of(0.5f, 0.7f));
        SkillChunkImportDto first = chunkDto(1L, 1, "Intro", List.of(0.1f, 0.2f));
        Path jsonl = tempDir.resolve("chunks.jsonl");
        Files.writeString(jsonl, "line-two\nline-one\n");

        when(objectMapper.readValue(eq("line-two"), eq(SkillChunkImportDto.class))).thenReturn(second);
        when(objectMapper.readValue(eq("line-one"), eq(SkillChunkImportDto.class))).thenReturn(first);
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));

        service.importFromJsonl(jsonl.toString());

        verify(skillChunkRepository).deleteBySkillId(1L);
        ArgumentCaptor<List> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(skillChunkRepository).saveAll(chunksCaptor.capture());

        List<SkillChunk> savedChunks = (List<SkillChunk>) chunksCaptor.getValue();
        assertThat(savedChunks).hasSize(2);
        assertThat(savedChunks.get(0).getChunkIndex()).isEqualTo(1);
        assertThat(savedChunks.get(0).getSectionTitle()).isEqualTo("Intro");
        assertThat(savedChunks.get(0).getEmbedding()).containsExactly(0.1f, 0.2f);
        assertThat(savedChunks.get(1).getChunkIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("importFromJsonl은 비어 있는 파일이면 아무 것도 저장하지 않는다")
    void importFromJsonl_returnsWhenFileHasNoRows() throws IOException {
        SkillChunkImportService service =
                new SkillChunkImportService(skillRepository, skillChunkRepository, objectMapper);
        Path jsonl = tempDir.resolve("empty.jsonl");
        Files.writeString(jsonl, "\n  \n");

        service.importFromJsonl(jsonl);

        verifyNoInteractions(skillRepository, skillChunkRepository, objectMapper);
    }

    @Test
    @DisplayName("importFromJsonl은 JSONL 파싱에 실패하면 예외를 던진다")
    void importFromJsonl_throwsWhenJsonParsingFails() throws IOException {
        SkillChunkImportService service =
                new SkillChunkImportService(skillRepository, skillChunkRepository, objectMapper);
        Path jsonl = tempDir.resolve("broken.jsonl");
        Files.writeString(jsonl, "broken-line");

        when(objectMapper.readValue(eq("broken-line"), eq(SkillChunkImportDto.class)))
                .thenThrow(new RuntimeException("bad json"));

        assertThatThrownBy(() -> service.importFromJsonl(jsonl))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("importFromJsonl은 없는 skill_id를 만나면 예외를 던진다")
    void importFromJsonl_throwsWhenSkillDoesNotExist() throws IOException {
        SkillChunkImportService service =
                new SkillChunkImportService(skillRepository, skillChunkRepository, objectMapper);
        SkillChunkImportDto dto = chunkDto(99L, 1, "Intro", List.of(0.1f));
        Path jsonl = tempDir.resolve("missing-skill.jsonl");
        Files.writeString(jsonl, "line-one");

        when(objectMapper.readValue(eq("line-one"), eq(SkillChunkImportDto.class))).thenReturn(dto);
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importFromJsonl(jsonl))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private SkillChunkImportDto chunkDto(Long skillId, Integer chunkIndex, String title, List<Float> embedding) {
        SkillChunkImportDto dto = new SkillChunkImportDto();
        ReflectionTestUtils.setField(dto, "skillId", skillId);
        ReflectionTestUtils.setField(dto, "chunkIndex", chunkIndex);
        ReflectionTestUtils.setField(dto, "sectionTitle", title);
        ReflectionTestUtils.setField(dto, "sectionPath", title.toLowerCase());
        ReflectionTestUtils.setField(dto, "searchText", title + " search");
        ReflectionTestUtils.setField(dto, "charCount", 100);
        ReflectionTestUtils.setField(dto, "chunkVersion", "v1");
        ReflectionTestUtils.setField(dto, "embeddingModel", "bge-m3");
        ReflectionTestUtils.setField(dto, "embeddedAt", OffsetDateTime.parse("2026-03-31T00:00:00Z"));
        ReflectionTestUtils.setField(dto, "embedding", embedding);
        return dto;
    }

    private Skill skill(Long id) {
        Skill skill = Skill.builder()
                .repository(repository())
                .name("alpha")
                .contentMd("alpha content")
                .contentHash("alpha-hash")
                .filePath("skills/alpha.md")
                .category(Category.BACKEND)
                .build();
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }

    private Repository repository() {
        return Repository.builder()
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
    }
}
