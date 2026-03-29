package back.domain.prompt.embedding.service;

import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.repository.SkillRepository;
import back.domain.prompt.embedding.dto.SkillEmbeddingExportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillEmbeddingExportService {

    private final SkillRepository skillRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.embedding.export-dir:./data/embedding}")
    private String exportDir;

    @Transactional(readOnly = true)
    public Path exportAllSkillsToJson() {
        List<Skill> skills = skillRepository.findAllByOrderByIdAsc();

        List<SkillEmbeddingExportDto> exportDtos = skills.stream()
                .map(this::toExportDto)
                .toList();

        try {
            Path dirPath = Path.of(exportDir);
            Files.createDirectories(dirPath);

            Path filePath = dirPath.resolve("skills_for_embedding.json");

            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(filePath.toFile(), exportDtos);

            log.info("[SkillEmbeddingExportService] export 완료. count={}, path={}",
                    exportDtos.size(), filePath);

            return filePath;
        } catch (IOException e) {
            throw new IllegalStateException("skills_for_embedding.json 생성 실패", e);
        }
    }

    @Transactional(readOnly = true)
    public List<SkillEmbeddingExportDto> getAllSkillsForExport() {
        return skillRepository.findAllByOrderByIdAsc().stream()
                .map(this::toExportDto)
                .toList();
    }

    private SkillEmbeddingExportDto toExportDto(Skill skill) {
        return SkillEmbeddingExportDto.builder()
                .skillId(skill.getId())
                .repositoryId(skill.getRepository() != null ? skill.getRepository().getId() : null)
                .name(skill.getName())
                .path(skill.getFilePath())
                .contentMd(skill.getContentMd())
                .contentHash(skill.getContentHash())
                .build();
    }
}
