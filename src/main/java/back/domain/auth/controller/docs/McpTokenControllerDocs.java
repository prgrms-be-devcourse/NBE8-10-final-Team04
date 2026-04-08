package back.domain.auth.controller.docs;

import org.springframework.http.ResponseEntity;

import back.domain.auth.dto.request.CreateMcpTokenRequest;
import back.domain.auth.dto.response.CreateMcpTokenResponse;
import back.domain.auth.dto.response.McpTokenListResponse;
import back.global.response.RsData;
import back.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "MCP Token", description = "사용자 JWT 기반 MCP 토큰 발급/조회/폐기 API")
@SecurityRequirement(name = "bearerAuth")
public interface McpTokenControllerDocs {

    @Operation(summary = "MCP 토큰 발급", description = "사용자가 MCP 연동에 사용할 평문 토큰을 신규 발급합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "발급 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": {
                            "tokenId": 12,
                            "token": "mcp_abcd1234example",
                            "tokenPrefix": "mcp_abcd1234",
                            "name": "Claude Desktop",
                            "expiresAt": "2026-04-30T00:00:00"
                          },
                          "message": "MCP 토큰 발급 성공"
                        }
                        """))),
        @ApiResponse(
                responseCode = "401",
                description = "JWT 인증 실패",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"로그인이 필요합니다.\"}"))),
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
                          "message": "name-NotBlank-name은 필수입니다."
                        }
                        """)))
    })
    ResponseEntity<RsData<CreateMcpTokenResponse>> issueToken(
            @Parameter(hidden = true) AuthenticatedMember authenticatedMember,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "토큰 이름과 만료 시각",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    value =
                                                            """
                                    {
                                      "name": "Claude Desktop",
                                      "expiresAt": "2026-04-30T00:00:00"
                                    }
                                    """)))
                    @Valid CreateMcpTokenRequest request);

    @Operation(summary = "MCP 토큰 목록 조회", description = "인증된 사용자의 MCP 토큰 목록을 조회합니다.")
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
                            "tokens": [
                              {
                                "tokenId": 12,
                                "name": "Codex",
                                "tokenPrefix": "mcp_hash_pre",
                                "expiresAt": "2026-04-30T00:00:00",
                                "lastUsedAt": null,
                                "revoked": false
                              }
                            ]
                          },
                          "message": "조회 성공"
                        }
                        """))),
        @ApiResponse(
                responseCode = "401",
                description = "JWT 인증 실패",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<RsData<McpTokenListResponse>> getTokens(
            @Parameter(hidden = true) AuthenticatedMember authenticatedMember);

    @Operation(summary = "MCP 토큰 폐기", description = "인증된 사용자의 특정 MCP 토큰을 폐기(revoked=true)합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "폐기 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"MCP 토큰 폐기 성공\"}"))),
        @ApiResponse(
                responseCode = "403",
                description = "본인 소유가 아닌 토큰",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"본인 토큰이 아닙니다.\"}"))),
        @ApiResponse(
                responseCode = "404",
                description = "토큰 없음",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"토큰이 존재하지 않습니다.\"}")))
    })
    ResponseEntity<RsData<Void>> revokeToken(
            @Parameter(hidden = true) AuthenticatedMember authenticatedMember,
            @Parameter(description = "폐기할 MCP 토큰 ID", example = "12")
                    @Positive(message = "tokenId-Positive-tokenId는 1 이상이어야 합니다.")
                    Long tokenId);
}
