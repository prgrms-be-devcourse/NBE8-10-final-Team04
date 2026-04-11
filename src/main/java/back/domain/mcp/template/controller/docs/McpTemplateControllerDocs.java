package back.domain.mcp.template.controller.docs;

import org.springframework.http.ResponseEntity;

import back.domain.mcp.template.dto.StartAgentTemplateRequest;
import back.domain.mcp.template.dto.StartAgentTemplateResponse;
import back.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "MCP Template", description = "MCP 시작 템플릿 조회 API")
@SecurityRequirement(name = "mcpBearerAuth")
public interface McpTemplateControllerDocs {

    @Operation(summary = "시작 템플릿 조회", description = "에이전트 타입(CODEX/CLAUDE/GEMINI)에 맞는 start.agent.md 템플릿을 반환합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": {
                            "templateName": "start.agent.md",
                            "version": "v7",
                            "templateMarkdown": "# START AGENT TEMPLATE (CODEX)\\n..."
                          },
                          "message": "템플릿 조회 성공"
                        }
                        """))),
        @ApiResponse(
                responseCode = "401",
                description = "MCP 토큰 인증 실패",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"MCP 토큰 인증 실패\"}"))),
        @ApiResponse(
                responseCode = "400",
                description = "요청 검증 실패",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": null,
                          "message": "agentType-NotNull-agentType은 필수입니다."
                        }
                        """)))
    })
    ResponseEntity<RsData<StartAgentTemplateResponse>> getStartAgentTemplate(
            @Parameter(
                            in = ParameterIn.HEADER,
                            name = "Authorization",
                            required = true,
                            description = "Bearer mcp_xxx",
                            example = "Bearer mcp_template_token_801")
                    String authorizationHeader,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "에이전트 타입 선택",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    value =
                                                            """
                                    {
                                      "agentType": "CODEX"
                                    }
                                    """)))
                    @Valid StartAgentTemplateRequest request);
}
