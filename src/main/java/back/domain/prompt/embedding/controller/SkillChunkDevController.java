package back.domain.prompt.embedding.controller;


import back.domain.prompt.embedding.service.SkillChunkImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev/skill-chunks")
@RequiredArgsConstructor
public class SkillChunkDevController {

    // [개발용] skill_chunks JSONL 파일을 저장하는 개발용 컨트롤러로, 홈서버와 연동 시 삭제됩니다.

    private final SkillChunkImportService skillChunkImportService;

    @PostMapping("/import")
    public ResponseEntity<String> importJsonl(@RequestParam("filePath") String filePath) {
        skillChunkImportService.importFromJsonl(filePath);
        return ResponseEntity.ok("skill_chunks import 완료");
    }
}