package back.domain.prompt.chunking.controller;

import back.domain.prompt.chunking.service.ChunkingService;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.service.SkillSearchService;
import back.global.response.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/skills")
public class SkillChunkController {

    private final ChunkingService chunkingService;

    @PostMapping("/chunk")
    public ResponseEntity<RsData<Void>> run() {
        chunkingService.chunk();

        return ResponseEntity.ok(new RsData<>("skill_chunk 적재 완료"));
    }

}
