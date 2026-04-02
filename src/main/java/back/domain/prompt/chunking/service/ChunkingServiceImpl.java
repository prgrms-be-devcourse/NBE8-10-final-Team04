package back.domain.prompt.chunking.service;

import back.domain.prompt.chunking.dto.Section;
import back.domain.prompt.chunking.entity.SkillChunk;
import back.domain.prompt.chunking.chunker.MarkdownChunker;
import back.domain.prompt.chunking.repository.SkillChunkRepository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.repository.SkillRepository;
import back.domain.prompt.search.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChunkingServiceImpl implements ChunkingService {

    private static final String CHUNK_VERSION = "v1";
    private static final String EMBEDDING_MODEL = "BAAI/bge-m3";

    private final SkillChunkRepository skillChunkRepository;
    private final SkillRepository skillRepository;
    private final EmbeddingService embeddingService;
    private final MarkdownChunker markdownChunker;

    @Override
    public void chunk() {

        // is_chunked = false인 skill들
        List<Skill> pending = skillRepository.findByIsChunkedFalse();

        for (Skill skill : pending) {
            // contentMd는 PromptServiceImpl이 OCI에서 읽어 DB에 저장한 값
            List<Section> sections = markdownChunker.chunkMarkdown(skill.getContentMd());

            // searchText에 메타 정보 붙이기
            List<String> searchTexts = sections.stream()
                    .map(s -> buildSearchText(skill.getName(), s.sectionTitle(), s.text()))
                    .toList();

            List<List<Float>> embeddings = embeddingService.embedBatch(searchTexts);

            List<SkillChunk> skillChunks = new ArrayList<>();
            for (int i = 0; i < sections.size(); i++) {
                skillChunks.add(SkillChunk.builder()
                        .skill(skill)
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

            skillChunkRepository.saveAll(skillChunks);
            skillRepository.markAsChunked(skill.getId());
        }
    }

    // search_text에 메타 정보 붙이기
    private String buildSearchText(String name, String sectionTitle, String chunkText) {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append("[skill: ").append(name).append("] ");
        }
        if (sectionTitle != null) {
            sb.append("[section: ").append(sectionTitle).append("] ");
        }
        sb.append("\n").append(chunkText);
        return sb.toString().strip();
    }
}
