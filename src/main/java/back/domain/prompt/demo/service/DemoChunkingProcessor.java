package back.domain.prompt.demo.service;

import back.domain.prompt.chunking.chunker.MarkdownChunker;
import back.domain.prompt.chunking.dto.Section;
import back.domain.prompt.chunking.service.EmbeddingService;
import back.domain.prompt.demo.entity.DemoSkill;
import back.domain.prompt.demo.entity.DemoSkillChunk;
import back.domain.prompt.demo.repository.DemoSkillChunkRepository;
import back.domain.prompt.demo.repository.DemoSkillRepository;
import back.domain.prompt.prompt.parser.SkillNormalizeParser;
import back.domain.prompt.search.util.VectorUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링 DI로 주입되는 공유 의존성은 서비스 필드로 보관합니다."
)
public class DemoChunkingProcessor {

    private static final String CHUNK_VERSION = "v2";
    private static final String EMBEDDING_MODEL = "BAAI/bge-m3";

    private final DemoSkillChunkRepository demoSkillChunkRepository;
    private final DemoSkillRepository demoSkillRepository;
    private final EmbeddingService embeddingService;
    private final MarkdownChunker markdownChunker;
    private final SkillNormalizeParser skillNormalizeParser;

    // DemoSkill 하나를 청킹·임베딩하고 demo_skill_chunks에 저장한다.
    @Transactional
    public void processOne(DemoSkill demoSkill) {
        // 1. 기존 청크 삭제 (재청킹 시 이전 데이터 정리)
        demoSkillChunkRepository.deleteByDemoSkillId(demoSkill.getId());

        // 2. 마크다운 청킹
        List<Section> sections = markdownChunker.chunkMarkdown(demoSkill.getContentMd());

        // 3. 검색용 텍스트 구성 후 임베딩 일괄 요청
        List<String> searchTexts = sections.stream()
                .map(s -> buildSearchText(demoSkill.getSkillName(), s.sectionTitle(), s.text()))
                .toList();
        List<List<Float>> embeddings = embeddingService.embedBatch(searchTexts);

        // 4. DemoSkillChunk 엔티티 생성 및 저장
        List<DemoSkillChunk> chunks = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            chunks.add(DemoSkillChunk.builder()
                    .demoSkill(demoSkill)
                    .chunkIndex(i)
                    .sectionTitle(sections.get(i).sectionTitle())
                    .searchText(searchTexts.get(i))
                    .charCount(sections.get(i).text().length())
                    .chunkVersion(CHUNK_VERSION)
                    .embeddingModel(EMBEDDING_MODEL)
                    .embedding(VectorUtils.toFloatArray(embeddings.get(i)))
                    .embeddedAt(OffsetDateTime.now())
                    .build());
        }
        demoSkillChunkRepository.saveAll(chunks);

        // 5. 청킹 완료 표시
        demoSkill.markChunked();
        demoSkillRepository.save(demoSkill);
    }

    private String buildSearchText(String name, String sectionTitle, String chunkText) {
        String meta = (name == null ? "" : name + " ") + (sectionTitle == null ? "" : sectionTitle);

        Set<String> keywords = skillNormalizeParser.extractTags(meta, chunkText);
        List<String> aliases = skillNormalizeParser.extractAliases(meta, chunkText);

        StringBuilder sb = new StringBuilder();

        if (name != null) {
            sb.append("[skill: ").append(name).append("] ");
        }

        if (sectionTitle != null) {
            sb.append("[section: ").append(sectionTitle).append("] ");
        }

        if (!keywords.isEmpty()) {
            sb.append("[keywords: ").append(String.join(" ", keywords)).append("] ");
        }

        if (!aliases.isEmpty()) {
            sb.append("[aliases: ").append(String.join(" ", aliases)).append("] ");
        }

        sb.append("\n").append(chunkText == null ? "" : chunkText);

        return sb.toString().strip();
    }
}
