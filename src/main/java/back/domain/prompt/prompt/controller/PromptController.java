package back.domain.prompt.prompt.controller;

import back.domain.prompt.prompt.dto.SkillDetailDto;
import back.domain.prompt.prompt.dto.SkillListItemDto;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.service.PromptService;
import back.domain.prompt.prompt.service.SkillReadService;
import back.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/prompts")
public class PromptController {
    private final PromptService promptService;
    private final SkillReadService skillReadService;

    @PostMapping("/run")
    public ResponseEntity<RsData<Void>> getPrompts() {
        promptService.run();
        return ResponseEntity.ok(new RsData<>("데이터 적재 완료"));
    }

    // 다건 조회
    @GetMapping("/skills")
    public ResponseEntity<Page<SkillListItemDto>> getSkills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(
                skillReadService.getSkills(PageRequest.of(page, size), category));
    }

    // 단건 조회
    @GetMapping("/skills/{id}")
    public ResponseEntity<SkillDetailDto> getSkill(@PathVariable long id) {
        return ResponseEntity.ok(skillReadService.getSkillDetail(id));
    }

    // 검색
    @GetMapping("/skills/search")
    public ResponseEntity<Page<SkillListItemDto>> searchSkills(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                skillReadService.searchSkills(query, PageRequest.of(page, size)));
    }
}
