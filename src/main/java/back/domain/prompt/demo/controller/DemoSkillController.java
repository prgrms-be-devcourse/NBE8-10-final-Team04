package back.domain.prompt.demo.controller;

import back.domain.prompt.demo.service.DemoSkillSearchService;
import back.domain.prompt.demo.service.DemoSkillSeedService;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.dto.request.SkillSearchRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/demo/skills")
public class DemoSkillController {

    private final DemoSkillSearchService demoSkillSearchService;
    private final DemoSkillSeedService demoSkillSeedService;

    @PostMapping("/search")
    public SkillChunkSearchResultDto search(@RequestBody SkillSearchRequestDto request) {
        return demoSkillSearchService.search(request.queries());
    }

    @PostMapping("/seed")
    public ResponseEntity<String> seed() {
        demoSkillSeedService.seed();
        return ResponseEntity.ok("데모 스킬 시드가 완료되었습니다.");
    }
}
