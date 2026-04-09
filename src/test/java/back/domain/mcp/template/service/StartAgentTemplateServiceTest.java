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
        assertThat(response.version()).isEqualTo("v6");
        assertThat(response.templateMarkdown()).contains("CLAUDE");
        assertTemplateContainsCoreFlowRules(response.templateMarkdown());
    }

    @Test
    @DisplayName("CODEX 템플릿을 조회할 수 있다")
    void getTemplate_codex() {
        StartAgentTemplateResponse response = startAgentTemplateService.getTemplate(AgentType.CODEX);

        assertThat(response.templateName()).isEqualTo("start.agent.md");
        assertThat(response.version()).isEqualTo("v6");
        assertThat(response.templateMarkdown()).contains("CODEX");
        assertTemplateContainsCoreFlowRules(response.templateMarkdown());
    }

    @Test
    @DisplayName("GEMINI 템플릿을 조회할 수 있다")
    void getTemplate_gemini() {
        StartAgentTemplateResponse response = startAgentTemplateService.getTemplate(AgentType.GEMINI);

        assertThat(response.templateName()).isEqualTo("start.agent.md");
        assertThat(response.version()).isEqualTo("v6");
        assertThat(response.templateMarkdown()).contains("GEMINI");
        assertTemplateContainsCoreFlowRules(response.templateMarkdown());
    }

    private static void assertTemplateContainsCoreFlowRules(String templateMarkdown) {
        assertThat(templateMarkdown).contains("start_auto_flow");
        assertThat(templateMarkdown).contains("자동화 워크 플로우 진행해줘");
        assertThat(templateMarkdown).contains("actions.writeFiles");
        assertThat(templateMarkdown).contains("start_auto_flow(step=COLLECTED");
        assertThat(templateMarkdown).contains("runner");
        assertThat(templateMarkdown).contains("generate_skills.py");
        assertThat(templateMarkdown).contains("요약/축약/재작성");
    }
}
