package back.domain.prompt.chunking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.prompt.chunking.dto.Section;
import back.domain.prompt.chunking.entity.SkillChunk;
import back.domain.prompt.chunking.chunker.MarkdownChunker;
import back.domain.prompt.chunking.repository.SkillChunkRepository;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;
import back.domain.prompt.prompt.repository.SkillRepository;

@ExtendWith(MockitoExtension.class)
class ChunkingServiceImplTest {

    @Mock
    private SkillChunkRepository skillChunkRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private MarkdownChunker markdownChunker;

    private ChunkingServiceImpl chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingServiceImpl(
                skillChunkRepository,
                skillRepository,
                embeddingService,
                markdownChunker
        );
    }

    @Test
    @DisplayName("청크 대상 skill이 있으면 search text와 embedding을 저장하고 chunked 상태로 바꾼다")
    void chunk_savesGeneratedChunksAndMarksSkillAsChunked() {
        Skill skill = skill(10L, "alpha", "# Intro\ncontent");
        List<Section> sections = List.of(
                new Section("Install", "install steps"),
                new Section(null, "plain body")
        );
        List<String> searchTexts = List.of(
                "[skill: alpha] [section: Install] \ninstall steps",
                "[skill: alpha] \nplain body"
        );

        when(skillRepository.findByIsChunkedFalse()).thenReturn(List.of(skill));
        when(markdownChunker.chunkMarkdown(skill.getContentMd())).thenReturn(sections);
        when(embeddingService.embedBatch(searchTexts)).thenReturn(List.of(
                List.of(0.1f, 0.2f),
                List.of(0.3f, 0.4f)
        ));

        chunkingService.chunk();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SkillChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillChunkRepository).saveAll(captor.capture());
        verify(skillRepository).markAsChunked(10L);

        List<SkillChunk> savedChunks = captor.getValue();
        assertThat(savedChunks).hasSize(2);

        assertThat(savedChunks.get(0).getSkill()).isSameAs(skill);
        assertThat(savedChunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(savedChunks.get(0).getSectionTitle()).isEqualTo("Install");
        assertThat(savedChunks.get(0).getSearchText()).isEqualTo(searchTexts.get(0));
        assertThat(savedChunks.get(0).getCharCount()).isEqualTo("install steps".length());
        assertThat(savedChunks.get(0).getChunkVersion()).isEqualTo("v1");
        assertThat(savedChunks.get(0).getEmbeddingModel()).isEqualTo("BAAI/bge-m3");
        assertThat(savedChunks.get(0).getEmbedding()).containsExactly(0.1f, 0.2f);
        assertThat(savedChunks.get(0).getEmbeddedAt()).isNotNull();

        assertThat(savedChunks.get(1).getChunkIndex()).isEqualTo(1);
        assertThat(savedChunks.get(1).getSectionTitle()).isNull();
        assertThat(savedChunks.get(1).getSearchText()).isEqualTo(searchTexts.get(1));
        assertThat(savedChunks.get(1).getEmbedding()).containsExactly(0.3f, 0.4f);
    }

    @Test
    @DisplayName("청크 대상 skill이 없으면 다른 의존성을 호출하지 않는다")
    void chunk_doesNothingWhenNoPendingSkillExists() {
        when(skillRepository.findByIsChunkedFalse()).thenReturn(List.of());

        chunkingService.chunk();

        verifyNoInteractions(markdownChunker, embeddingService, skillChunkRepository);
        verify(skillRepository, never()).markAsChunked(anyLong());
    }

    private Skill skill(Long id, String name, String contentMd) {
        Repository repository = Repository.builder()
                .githubId(1L)
                .name("demo-repo")
                .sourceRepo("owner/repo")
                .sourceUri("https://example.com/owner/repo")
                .summary("summary")
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
                .sourceUpdatedAt(LocalDateTime.parse("2026-03-25T00:00:00"))
                .active(true)
                .rawMetadata(Map.of("k", "v"))
                .build();

        Skill skill = Skill.builder()
                .repository(repository)
                .name(name)
                .contentMd(contentMd)
                .contentHash("hash")
                .filePath("skills/%s.md".formatted(name))
                .category(Category.BACKEND)
                .tagsJson(Set.of("spring"))
                .build();

        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }
}
