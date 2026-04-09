package back.domain.prompt.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import back.domain.prompt.demo.service.DemoSkillSearchService;
import back.domain.prompt.demo.service.DemoSkillSeedService;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.dto.request.SkillSearchRequestDto;

class DemoSkillControllerTest {

    @Test
    @DisplayName("search는 DemoSkillSearchService 결과를 그대로 반환한다")
    void search_returnsServiceResult() {
        DemoSkillSearchService searchService = mock(DemoSkillSearchService.class);
        DemoSkillSeedService seedService = mock(DemoSkillSeedService.class);
        DemoSkillController controller = new DemoSkillController(searchService, seedService);

        SkillSearchRequestDto request = new SkillSearchRequestDto(List.of("spring", "boot"));
        SkillChunkSearchResultDto expected = new SkillChunkSearchResultDto(List.of());

        when(searchService.search(List.of("spring", "boot"))).thenReturn(expected);

        SkillChunkSearchResultDto response = controller.search(request);

        assertThat(response).isSameAs(expected);
        verify(searchService).search(List.of("spring", "boot"));
    }

    @Test
    @DisplayName("seed는 시드 실행 후 성공 응답을 반환한다")
    void seed_runsSeedAndReturnsOk() {
        DemoSkillSearchService searchService = mock(DemoSkillSearchService.class);
        DemoSkillSeedService seedService = mock(DemoSkillSeedService.class);
        DemoSkillController controller = new DemoSkillController(searchService, seedService);

        ResponseEntity<String> response = controller.seed();

        verify(seedService).seed();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("완료");
    }
}
