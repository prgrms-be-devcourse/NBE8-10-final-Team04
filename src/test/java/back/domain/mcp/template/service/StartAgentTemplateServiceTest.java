package back.domain.mcp.template.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import back.domain.mcp.template.dto.AgentType;
import back.domain.mcp.template.dto.StartAgentTemplateResponse;

class StartAgentTemplateServiceTest {

    private final StartAgentTemplateService startAgentTemplateService =
            new StartAgentTemplateServiceImpl(new DefaultResourceLoader());

    @Test
    @DisplayName("CLAUDE 템플릿을 조회할 수 있다")
    void getTemplate_claude() {
        StartAgentTemplateResponse response = startAgentTemplateService.getTemplate(AgentType.CLAUDE);

        assertThat(response.templateName()).isEqualTo("start.agent.md");
        assertThat(response.version()).isEqualTo("v1");
        assertThat(response.templateMarkdown()).contains("CLAUDE");
        assertThat(response.templateMarkdown()).contains("agents.md");
    }

    @Test
    @DisplayName("CODEX 템플릿을 조회할 수 있다")
    void getTemplate_codex() {
        StartAgentTemplateResponse response = startAgentTemplateService.getTemplate(AgentType.CODEX);

        assertThat(response.templateName()).isEqualTo("start.agent.md");
        assertThat(response.version()).isEqualTo("v1");
        assertThat(response.templateMarkdown()).contains("CODEX");
        assertThat(response.templateMarkdown()).contains("agents.md");
    }

    @Test
    @DisplayName("GEMINI 템플릿을 조회할 수 있다")
    void getTemplate_gemini() {
        StartAgentTemplateResponse response = startAgentTemplateService.getTemplate(AgentType.GEMINI);

        assertThat(response.templateName()).isEqualTo("start.agent.md");
        assertThat(response.version()).isEqualTo("v1");
        assertThat(response.templateMarkdown()).contains("GEMINI");
        assertThat(response.templateMarkdown()).contains("agents.md");
    }
}
