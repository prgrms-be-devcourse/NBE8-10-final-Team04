package back.domain.prompt.prompt.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import back.domain.prompt.prompt.dto.SkillDetailDto;
import back.domain.prompt.prompt.dto.SkillListItemDto;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.service.PromptService;
import back.domain.prompt.prompt.service.SkillReadService;
import back.global.response.RsData;

class PromptControllerTest {

    private PromptService promptService;
    private SkillReadService skillReadService;
    private PromptController promptController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        promptService = mock(PromptService.class);
        skillReadService = mock(SkillReadService.class);
        promptController = new PromptController(promptService, skillReadService);
        mockMvc = MockMvcBuilders.standaloneSetup(promptController).build();
    }

    @Test
    @DisplayName("getPrompts는 PromptService를 실행하고 성공 응답을 반환한다")
    void getPrompts_returnsSuccess() {
        ResponseEntity<RsData<Void>> response = promptController.getPrompts();
        RsData<Void> body = response.getBody();

        verify(promptService).run();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(body).isNotNull();
        assertThat(body.data()).isNull();
        assertThat(body.message()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/v1/prompts/run은 성공 응답을 반환한다")
    void runEndpoint_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/prompts/run").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", notNullValue()));

        verify(promptService).run();
    }

    @Test
    @DisplayName("GET /api/v1/prompts/skills는 페이지 목록을 반환한다")
    void getSkillsEndpoint_returnsPage() throws Exception {
        SkillListItemDto item = new SkillListItemDto(
                1L,
                "springboot",
                Category.BACKEND.name(),
                Set.of("spring"),
                "demo-repo",
                "https://example.com/repo",
                "summary",
                10,
                2
        );
        Page<SkillListItemDto> page = new PageImpl<>(java.util.List.of(item), PageRequest.of(0, 10), 1);
        when(skillReadService.getSkills(PageRequest.of(0, 10), null)).thenReturn(page);

        mockMvc.perform(get("/api/v1/prompts/skills?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("springboot")));
    }

    @Test
    @DisplayName("GET /api/v1/prompts/skills/{id}는 상세 정보를 반환한다")
    void getSkillEndpoint_returnsDetail() throws Exception {
        SkillDetailDto detail = new SkillDetailDto(
                1L,
                "springboot",
                Category.BACKEND.name(),
                Set.of("spring"),
                "demo-repo",
                "https://example.com/repo",
                "summary",
                10,
                2,
                "# content"
        );
        when(skillReadService.getSkillDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/prompts/skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("springboot")));
    }

    @Test
    @DisplayName("GET /api/v1/prompts/skills/search는 검색 페이지를 반환한다")
    void searchSkillsEndpoint_returnsPage() throws Exception {
        SkillListItemDto item = new SkillListItemDto(
                2L,
                "querydsl",
                Category.BACKEND.name(),
                Set.of("querydsl"),
                "demo-repo",
                "https://example.com/repo",
                "summary",
                20,
                3
        );
        Page<SkillListItemDto> page = new PageImpl<>(java.util.List.of(item), PageRequest.of(0, 10), 1);
        when(skillReadService.searchSkills("query", PageRequest.of(0, 10))).thenReturn(page);

        mockMvc.perform(get("/api/v1/prompts/skills/search?query=query&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(2)))
                .andExpect(jsonPath("$.content[0].name", is("querydsl")));
    }
}
