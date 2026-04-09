package back.domain.communitypost.controller.docs;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import back.domain.communitypost.dto.AdminChangeCommunityPostStatusRequest;
import back.domain.communitypost.dto.AdminGenerateCommunityPostRequest;
import back.domain.communitypost.dto.AdminUpdateCommunityPostRequest;
import back.domain.communitypost.dto.CommunityPostInfoResponse;
import back.domain.communitypost.dto.PageCommunityPostResponse;
import back.domain.communitypost.entity.CommunityPostStatus;
import back.global.response.RsData;
import back.global.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Admin CommunityPost", description = "관리자 게시글 생성/수정/상태변경/삭제/목록 조회 API")
@SecurityRequirement(name = "bearerAuth")
public interface AdminCommunityPostControllerDocs {

    @Operation(
            summary = "관리자 게시글 목록 조회",
            description = "상태 필터와 페이징으로 전체 게시글을 조회합니다.",
            parameters = {
                @Parameter(
                        name = "status",
                        in = ParameterIn.QUERY,
                        description = "필터 상태값(PENDING_REVIEW/PUBLISHED/HIDDEN/REJECTED)",
                        schema = @Schema(implementation = CommunityPostStatus.class)),
                @Parameter(name = "page", in = ParameterIn.QUERY, description = "페이지 번호(0부터)", example = "0"),
                @Parameter(name = "size", in = ParameterIn.QUERY, description = "페이지 크기", example = "20"),
                @Parameter(
                        name = "sort",
                        in = ParameterIn.QUERY,
                        description = "정렬 조건(기본 createdAt,DESC)",
                        example = "createdAt,desc")
            })
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
                            "contents": [
                              {
                                "id": 5,
                                "type": "MODEL_INFO",
                                "title": "2026-04-09 OpenAI 모델 정보 업데이트",
                                "summary": "GPT-5.4 변경사항 요약",
                                "body": "# 2026-04-09 OpenAI 모델 정보 업데이트\\n\\n- 벤더: OpenAI\\n- 총 변경 요청 수: 2\\n\\n## 패밀리: GPT-5.4\\n- 요약: GPT-5.4 변경사항 요약\\n- 소스 타입: RSS\\n- 소스 URL: https://news.example.com/openai/gpt-5-4\\n\\n원문 본문 내용...",
                                "status": "PENDING_REVIEW",
                                "targetDate": "2026-04-09",
                                "vendorId": 10,
                                "publishedAt": null,
                                "createdAt": "2026-04-09T10:00:00",
                                "updatedAt": "2026-04-09T10:00:00"
                              }
                            ],
                            "totalElements": 1,
                            "totalPages": 1,
                            "page": 0,
                            "size": 20
                          },
                          "message": "관리자 게시글 목록 조회 성공"
                        }
                        """))),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<RsData<PageCommunityPostResponse>> getAdminPosts(
            @Parameter(hidden = true) CommunityPostStatus status, @Parameter(hidden = true) Pageable pageable);

    @Operation(
            summary = "게시글 자동 생성",
            description = "타입과 날짜(필요 시 vendorId)로 PENDING_REVIEW 게시글을 생성합니다. MODEL_INFO는 update_requests(승인 상태) 기반으로 summary/body를 생성합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "생성 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": {
                            "id": 11,
                            "type": "MODEL_INFO",
                            "title": "2026-04-09 OpenAI 모델 정보 업데이트",
                            "summary": "GPT-5.4 변경사항 요약",
                            "body": "# 2026-04-09 OpenAI 모델 정보 업데이트\\n\\n- 벤더: OpenAI\\n- 총 변경 요청 수: 2\\n\\n## 패밀리: GPT-5.4\\n- 요약: GPT-5.4 변경사항 요약\\n- 소스 타입: RSS\\n- 소스 URL: https://news.example.com/openai/gpt-5-4\\n\\n원문 본문 내용...",
                            "status": "PENDING_REVIEW",
                            "targetDate": "2026-04-09",
                            "vendorId": 10,
                            "publishedAt": null,
                            "createdAt": "2026-04-09T10:30:00",
                            "updatedAt": "2026-04-09T10:30:00"
                          },
                          "message": "게시글 생성 성공"
                        }
                        """))),
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
                          "message": "MODEL_INFO 게시글 생성 시 vendorId는 필수입니다."
                        }
                        """))),
        @ApiResponse(
                responseCode = "404",
                description = "원천 데이터 없음",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": null,
                          "message": "해당 벤더/날짜의 업데이트 원천 데이터가 없습니다."
                        }
                        """))),
        @ApiResponse(
                responseCode = "409",
                description = "중복 생성 충돌",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": null,
                          "message": "해당 날짜와 타입의 게시글이 이미 존재합니다."
                        }
                        """)))
    })
    ResponseEntity<RsData<CommunityPostInfoResponse>> generatePost(
            @Parameter(hidden = true) AuthenticatedMember authenticatedMember,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "생성 타입, 날짜, 벤더ID(MODEL_INFO인 경우 필수)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    value =
                                                            """
                                    {
                                      "type": "MODEL_INFO",
                                      "targetDate": "2026-04-08",
                                      "vendorId": 10
                                    }
                                    """)))
                    @Valid AdminGenerateCommunityPostRequest request);

    @Operation(summary = "게시글 수정", description = "관리자가 게시글 제목/요약/본문을 수정합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "수정 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": {
                            "id": 11,
                            "title": "수정 제목"
                          },
                          "message": "게시글 수정 성공"
                        }
                        """))),
        @ApiResponse(
                responseCode = "404",
                description = "게시글 없음",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"게시글을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<RsData<CommunityPostInfoResponse>> updatePost(
            @Parameter(hidden = true) AuthenticatedMember authenticatedMember,
            @Parameter(description = "게시글 ID", example = "11") Long postId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "수정할 게시글 내용",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    value =
                                                            """
                                    {
                                      "title": "수정 제목",
                                      "summary": "수정 요약",
                                      "body": "# 수정 본문\\n내용..."
                                    }
                                    """)))
                    @Valid AdminUpdateCommunityPostRequest request);

    @Operation(summary = "게시글 상태 변경", description = "PENDING_REVIEW/PUBLISHED/HIDDEN/REJECTED 상태로 변경합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "상태 변경 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": {
                            "id": 11,
                            "status": "PUBLISHED"
                          },
                          "message": "게시글 상태 변경 성공"
                        }
                        """))),
        @ApiResponse(
                responseCode = "400",
                description = "잘못된 상태값",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": null,
                          "message": "status-NotNull-게시글 상태는 필수입니다."
                        }
                        """)))
    })
    ResponseEntity<RsData<CommunityPostInfoResponse>> changeStatus(
            @Parameter(hidden = true) AuthenticatedMember authenticatedMember,
            @Parameter(description = "게시글 ID", example = "11") Long postId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "변경할 상태값",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    value =
                                                            """
                                    {
                                      "status": "PUBLISHED"
                                    }
                                    """)))
                    @Valid AdminChangeCommunityPostStatusRequest request);

    @Operation(summary = "게시글 삭제(소프트 삭제)", description = "게시글을 하드 삭제하지 않고 상태를 HIDDEN으로 변경합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "삭제 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"게시글 삭제 성공\"}"))),
        @ApiResponse(
                responseCode = "404",
                description = "게시글 없음",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"게시글을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<RsData<Void>> deletePost(
            @Parameter(hidden = true) AuthenticatedMember authenticatedMember,
            @Parameter(description = "게시글 ID", example = "11") Long postId);
}
