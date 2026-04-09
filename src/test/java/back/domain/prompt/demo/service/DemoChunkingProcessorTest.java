package back.domain.prompt.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.prompt.chunking.chunker.MarkdownChunker;
import back.domain.prompt.chunking.dto.Section;
import back.domain.prompt.chunking.service.EmbeddingService;
import back.domain.prompt.demo.entity.DemoSkill;
import back.domain.prompt.demo.entity.DemoSkillChunk;
import back.domain.prompt.demo.repository.DemoSkillChunkRepository;
import back.domain.prompt.demo.repository.DemoSkillRepository;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.parser.SkillNormalizeParser;

@ExtendWith(MockitoExtension.class)
class DemoChunkingProcessorTest {

    @Mock
    private DemoSkillChunkRepository demoSkillChunkRepository;

    @Mock
    private DemoSkillRepository demoSkillRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private MarkdownChunker markdownChunker;

    @Mock
    private SkillNormalizeParser skillNormalizeParser;

    private DemoChunkingProcessor demoChunkingProcessor;

    @BeforeEach
    void setUp() {
        demoChunkingProcessor = new DemoChunkingProcessor(
                demoSkillChunkRepository,
                demoSkillRepository,
                embeddingService,
                markdownChunker,
                skillNormalizeParser
        );
    }

    @Test
    @DisplayName("processOne은 기존 청크 삭제 후 재청킹하고 chunked 상태로 저장한다")
    void processOne_replacesChunksAndMarksChunked() {
        DemoSkill demoSkill = demoSkill(10L, "alpha", "# Intro\ncontent");
        List<Section> sections = List.of(
                new Section("Install", "install steps"),
                new Section(null, "plain body")
        );
        List<String> searchTexts = List.of(
                "[skill: alpha] [section: Install] \ninstall steps",
                "[skill: alpha] \nplain body"
        );

        when(markdownChunker.chunkMarkdown(demoSkill.getContentMd())).thenReturn(sections);
        when(skillNormalizeParser.extractTags(anyString(), anyString())).thenReturn(Set.of());
        when(skillNormalizeParser.extractAliases(anyString(), anyString())).thenReturn(List.of());
        when(embeddingService.embedBatch(searchTexts)).thenReturn(List.of(
                List.of(0.1f, 0.2f),
                List.of(0.3f, 0.4f)
        ));

        demoChunkingProcessor.processOne(demoSkill);

        InOrder inOrder = inOrder(demoSkillChunkRepository, embeddingService, demoSkillRepository);
        inOrder.verify(demoSkillChunkRepository).deleteByDemoSkillId(10L);
        inOrder.verify(embeddingService).embedBatch(searchTexts);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DemoSkillChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(demoSkillChunkRepository).saveAll(captor.capture());
        verify(demoSkillRepository).save(demoSkill);

        List<DemoSkillChunk> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(saved.get(0).getChunkVersion()).isEqualTo("v2");
        assertThat(saved.get(0).getEmbeddingModel()).isEqualTo("BAAI/bge-m3");
        assertThat(saved.get(0).getEmbedding()).containsExactly(0.1f, 0.2f);
        assertThat(saved.get(1).getChunkIndex()).isEqualTo(1);
        assertThat(saved.get(1).getEmbedding()).containsExactly(0.3f, 0.4f);
        assertThat(demoSkill.isChunked()).isTrue();
    }

    private DemoSkill demoSkill(Long id, String skillName, String contentMd) {
        DemoSkill demoSkill = DemoSkill.builder()
                .skillId(1000L)
                .skillName(skillName)
                .repositoryName("demo-repo")
                .repositoryUrl("https://example.com/repo")
                .summary("summary")
                .contentMd(contentMd)
                .category(Category.BACKEND)
                .isChunked(false)
                .forks(1)
                .stars(10)
                .sourceUpdatedAt(OffsetDateTime.parse("2026-04-09T00:00:00Z"))
                .build();

        ReflectionTestUtils.setField(demoSkill, "id", id);
        return demoSkill;
    }
}
