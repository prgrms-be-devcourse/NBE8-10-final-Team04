package back.domain.prompt.embedding.controller;

import back.domain.prompt.embedding.dto.SkillEmbeddingExportDto;
import back.domain.prompt.embedding.service.SkillEmbeddingExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/dev/skills")
@RequiredArgsConstructor
public class SkillEmbeddingDevController {

    private final SkillEmbeddingExportService skillEmbeddingExportService;

    /**
     * JSON 응답으로 확인
     */
    @GetMapping("/export")
    public ResponseEntity<List<SkillEmbeddingExportDto>> exportSkillsAsJson() {
        List<SkillEmbeddingExportDto> result = skillEmbeddingExportService.getAllSkillsForExport();
        return ResponseEntity.ok(result);
    }

    /**
     * 파일 생성 후 다운로드 응답
     */
    @GetMapping("/export/file")
    public ResponseEntity<byte[]> exportSkillsAsFile() throws Exception {
        Path filePath = skillEmbeddingExportService.exportAllSkillsToJson();
        byte[] fileBytes = Files.readAllBytes(filePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"skills_for_embedding.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(fileBytes);
    }
}
