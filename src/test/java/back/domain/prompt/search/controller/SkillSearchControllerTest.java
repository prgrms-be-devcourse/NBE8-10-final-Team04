package back.domain.prompt.search.controller;

import back.domain.prompt.chunking.controller.SkillChunkController;
import back.domain.prompt.chunking.service.ChunkingService;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.service.SkillSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillSearchControllerTest {

    @Test
    @DisplayName("search는 서비스 결과를 그대로 반환한다")
    void search_returnsServiceResult() {
        SkillSearchService skillSearchService = mock(SkillSearchService.class);
        SkillChunkController controller = new SkillChunkController(mock(ChunkingService.class), skillSearchService);
        SkillChunkSearchResultDto result = new SkillChunkSearchResultDto(List.of());
        when(skillSearchService.search("spring")).thenReturn(result);

        SkillChunkSearchResultDto response = controller.search("spring");

        verify(skillSearchService).search("spring");
        assertThat(response).isSameAs(result);
    }
}
