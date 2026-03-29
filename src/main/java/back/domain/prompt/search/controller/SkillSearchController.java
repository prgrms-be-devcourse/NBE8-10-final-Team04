package back.domain.prompt.search.controller;

import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.service.SkillSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SkillSearchController {

    private final SkillSearchService skillSearchService;

    // 사실상 필요 없음?
    @GetMapping("/api/skills/search")
    public SkillChunkSearchResultDto search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK
    ) {
        return skillSearchService.search(query, topK);
    }
}
