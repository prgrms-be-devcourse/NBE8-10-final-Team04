package back.domain.prompt.search.controller;

import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.dto.request.SkillSearchRequestDto;
import back.domain.prompt.search.service.SkillSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/skills")
public class SkillSearchController {

    private final SkillSearchService skillSearchService;

    @PostMapping("/search")
    public SkillChunkSearchResultDto search(@RequestBody SkillSearchRequestDto request) {
        return skillSearchService.search(request.queries());
    }
}
