package back.domain.prompt.search.controller;


import back.domain.prompt.embedding.service.SkillChunkImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev/skill-chunks")
@RequiredArgsConstructor
public class SkillChunkDevController {

    private final SkillChunkImportService skillChunkImportService;

    @PostMapping("/import")
    public ResponseEntity<String> importJsonl(@RequestParam("filePath") String filePath) {
        skillChunkImportService.importFromJsonl(filePath);
        return ResponseEntity.ok("skill_chunks import 완료");
    }
}