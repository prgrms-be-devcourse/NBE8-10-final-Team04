package back.domain.prompt.embedding.service;

import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.repository.SkillRepository;
import back.domain.prompt.embedding.dto.SkillChunkImportDto;
import back.domain.prompt.embedding.entity.SkillChunk;
import back.domain.prompt.embedding.repository.SkillChunkRepository;
import back.domain.prompt.search.util.VectorUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "공유 Repository와 ObjectMapper 빈은 스프링이 주입하고 관리한다."
)
public class SkillChunkImportService {

    private final SkillRepository skillRepository;
    private final SkillChunkRepository skillChunkRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void importFromJsonl(String filePath) {
        importFromJsonl(Path.of(filePath));
    }

    @Transactional
    public void importFromJsonl(Path filePath) {
        List<SkillChunkImportDto> rows = readJsonl(filePath);

        if (rows.isEmpty()) {
            log.warn("[SkillChunkImportService] import 대상이 없습니다. path={}", filePath);
            return;
        }

        Map<Long, List<SkillChunkImportDto>> groupedBySkillId = rows.stream()
                .filter(row -> row.getSkillId() != null)
                .collect(Collectors.groupingBy(SkillChunkImportDto::getSkillId));

        int deletedSkillCount = 0;
        int savedChunkCount = 0;

        for (Map.Entry<Long, List<SkillChunkImportDto>> entry : groupedBySkillId.entrySet()) {
            Long skillId = entry.getKey();
            List<SkillChunkImportDto> chunkDtos = entry.getValue();

            Skill skill = skillRepository.findById(skillId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 skill_id 입니다. skillId=" + skillId));

            skillChunkRepository.deleteBySkillId(skillId);
            deletedSkillCount++;

            List<SkillChunk> chunks = chunkDtos.stream()
                    .sorted(Comparator.comparing(SkillChunkImportDto::getChunkIndex))
                    .map(dto -> toEntity(skill, dto))
                    .toList();

            skillChunkRepository.saveAll(chunks);
            savedChunkCount += chunks.size();

            log.info("[SkillChunkImportService] skill chunk 교체 완료. skillId={}, chunkCount={}",
                    skillId, chunks.size());
        }

        log.info("[SkillChunkImportService] import 완료. filePath={}, skillCount={}, chunkCount={}",
                filePath, deletedSkillCount, savedChunkCount);
    }

    private List<SkillChunkImportDto> readJsonl(Path filePath) {
        List<SkillChunkImportDto> result = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                try {
                    SkillChunkImportDto dto = objectMapper.readValue(line, SkillChunkImportDto.class);
                    result.add(dto);
                } catch (Exception e) {
                    log.warn("[SkillChunkImportService] JSONL 파싱 실패. path={}, lineNumber={}, line={}",
                            filePath, lineNumber, line);
                    throw new IllegalStateException(
                            "JSONL 파싱 실패. lineNumber=" + lineNumber + ", path=" + filePath, e);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("JSONL 파일 읽기 실패. path=" + filePath, e);
        }

        return result;
    }

    private SkillChunk toEntity(Skill skill, SkillChunkImportDto dto) {
        return SkillChunk.builder()
                .skill(skill)
                .chunkIndex(dto.getChunkIndex())
                .sectionTitle(dto.getSectionTitle())
                .sectionPath(dto.getSectionPath())
                .searchText(dto.getSearchText())
                .charCount(dto.getCharCount())
                .chunkVersion(dto.getChunkVersion())
                .embeddingModel(dto.getEmbeddingModel())
                .embedding(VectorUtils.toFloatArray(dto.getEmbedding()))
                .embeddedAt(dto.getEmbeddedAt())
                .build();
    }
}
