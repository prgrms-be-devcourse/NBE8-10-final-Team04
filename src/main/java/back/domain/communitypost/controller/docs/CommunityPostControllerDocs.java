package back.domain.communitypost.controller.docs;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import back.domain.communitypost.dto.CommunityPostInfoResponse;
import back.domain.communitypost.dto.PageCommunityPostResponse;
import back.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "CommunityPost", description = "공개 게시글 조회 API")
public interface CommunityPostControllerDocs {

    @Operation(
            summary = "공개 게시글 목록 조회",
            description = "PUBLISHED 상태 게시글 목록을 페이징 조회합니다.",
            parameters = {
                @Parameter(name = "page", in = ParameterIn.QUERY, description = "페이지 번호(0부터)", example = "0"),
                @Parameter(name = "size", in = ParameterIn.QUERY, description = "페이지 크기", example = "10"),
                @Parameter(
                        name = "sort",
                        in = ParameterIn.QUERY,
                        description = "정렬 조건(기본 publishedAt,DESC)",
                        example = "publishedAt,desc")
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
                                "id": 11,
                                "type": "MODEL_INFO",
                                "title": "2026-04-09 OpenAI 모델 정보 업데이트",
                                "summary": "GPT-5.4 변경사항 요약",
                                "body": "# 2026-04-09 OpenAI 모델 정보 업데이트\\n\\n- 벤더: OpenAI\\n- 총 변경 요청 수: 2\\n\\n## 패밀리: GPT-5.4\\n- 요약: GPT-5.4 변경사항 요약\\n- 소스 타입: RSS\\n- 소스 URL: https://news.example.com/openai/gpt-5-4\\n\\n원문 본문 내용...",
                                "status": "PUBLISHED",
                                "targetDate": "2026-04-09",
                                "vendorId": 10,
                                "publishedAt": "2026-04-09T11:00:00",
                                "createdAt": "2026-04-09T10:30:00",
                                "updatedAt": "2026-04-09T11:00:00"
                              }
                            ],
                            "totalElements": 1,
                            "totalPages": 1,
                            "page": 0,
                            "size": 10
                          },
                          "message": "게시글 목록 조회 성공"
                        }
                        """)))
    })
    ResponseEntity<RsData<PageCommunityPostResponse>> getPublicPosts(@Parameter(hidden = true) Pageable pageable);

    @Operation(summary = "공개 게시글 상세 조회", description = "PUBLISHED 상태의 게시글 단건을 조회합니다.")
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
                            "id": 11,
                            "type": "MODEL_INFO",
                            "title": "2026-04-09 OpenAI 모델 정보 업데이트",
                            "summary": "GPT-5.4 변경사항 요약",
                            "body": "# 2026-04-09 OpenAI 모델 정보 업데이트\\n\\n- 벤더: OpenAI\\n- 총 변경 요청 수: 2\\n\\n## 패밀리: GPT-5.4\\n- 요약: GPT-5.4 변경사항 요약\\n- 소스 타입: RSS\\n- 소스 URL: https://news.example.com/openai/gpt-5-4\\n\\n원문 본문 내용...",
                            "status": "PUBLISHED",
                            "targetDate": "2026-04-09",
                            "vendorId": 10,
                            "publishedAt": "2026-04-09T11:00:00",
                            "createdAt": "2026-04-09T10:30:00",
                            "updatedAt": "2026-04-09T11:00:00"
                          },
                          "message": "게시글 조회 성공"
                        }
                        """))),
        @ApiResponse(
                responseCode = "404",
                description = "게시글 없음 또는 비공개 상태",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"게시글을 찾을 수 없습니다.\"}")))
    })
    ResponseEntity<RsData<CommunityPostInfoResponse>> getPublicPost(
            @Parameter(description = "게시글 ID", example = "11") Long postId);
}
