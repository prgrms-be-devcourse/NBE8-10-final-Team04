package back.domain.mcp.recommendation.controller.docs;

import org.springframework.http.ResponseEntity;

import back.domain.mcp.recommendation.dto.McpRecommendationRequest;
import back.domain.mcp.recommendation.dto.McpRecommendationResponse;
import back.domain.mcp.recommendation.dto.McpRecommendationSkillContentRequest;
import back.domain.mcp.recommendation.dto.McpRecommendationSkillContentResponse;
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

@Tag(name = "MCP Recommendation", description = "queries 배열 기반 추천/스킬 본문 조회 API")
@SecurityRequirement(name = "mcpBearerAuth")
public interface McpRecommendationControllerDocs {

    @Operation(
            summary = "스킬 추천",
            description = "queries 배열로 후보를 검색하고 "
                    + "카테고리별 최종 추천 스킬 메타 정보를 반환합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "추천 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": {
                            "selectedSkills": [
                              {
                                "skillId": 1,
                                "category": "backend",
                                "finalScore": 0.8983,
                                "scoreBreakdown": {
                                  "primaryScore": 0.83,
                                  "starsNorm": 1.0,
                                  "forksNorm": 0.91,
                                  "freshnessNorm": 0.68
                                },
                                "sourceRepo": "example/doc-agent"
                              }
                            ]
                          },
                          "message": "추천 성공"
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
                          "message": "queries-NotEmpty-queries는 최소 1개 이상이어야 합니다."
                        }
                        """)))
    })
    ResponseEntity<RsData<McpRecommendationResponse>> recommend(
            @Parameter(
                            in = ParameterIn.HEADER,
                            name = "Authorization",
                            required = true,
                            description = "Bearer mcp_xxx",
                            example = "Bearer mcp_recommend_token_901")
                    String authorizationHeader,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "사용자 기획을 요약한 query 배열",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    value =
                                                            """
                                    {
                                      "queries": ["SpringBoot", "infra", "DevOps"]
                                    }
                                    """)))
                    @Valid McpRecommendationRequest request);

    @Operation(
            summary = "스킬 본문 조회",
            description = "추천 목록에서 선택한 skillId의 원문(md)을 조회합니다.")
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
                            "skillId": 10,
                            "category": "backend",
                            "sourceRepo": "example/skill-repo",
                            "skillMdRaw": "# backend skill content"
                          },
                          "message": "스킬 본문 조회 성공"
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
                          "message": "skillId-Positive-skillId는 1 이상의 값이어야 합니다."
                        }
                        """)))
    })
    ResponseEntity<RsData<McpRecommendationSkillContentResponse>> getSkillContent(
            @Parameter(
                            in = ParameterIn.HEADER,
                            name = "Authorization",
                            required = true,
                            description = "Bearer mcp_xxx",
                            example = "Bearer mcp_recommend_token_903")
                    String authorizationHeader,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "조회할 스킬 ID",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    value =
                                                            """
                                    {
                                      "skillId": 10
                                    }
                                    """)))
                    @Valid McpRecommendationSkillContentRequest request);
}
