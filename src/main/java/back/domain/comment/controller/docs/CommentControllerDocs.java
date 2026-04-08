package back.domain.comment.controller.docs;

import java.util.List;

import org.springframework.http.ResponseEntity;

import back.domain.comment.dto.CommentInfoResponse;
import back.domain.comment.dto.CreateCommentRequest;
import back.domain.comment.dto.UpdateCommentRequest;
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

@Tag(name = "Comment", description = "커뮤니티 댓글/대댓글 API")
public interface CommentControllerDocs {

    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글/대댓글 목록을 조회합니다.")
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
                          "data": [
                            {
                              "id": 1,
                              "postId": 11,
                              "authorId": 2,
                              "authorName": "user",
                              "parentId": null,
                              "depth": 0,
                              "content": "루트 댓글",
                              "deleted": false,
                              "createdAt": "2026-04-08T11:20:00",
                              "updatedAt": "2026-04-08T11:20:00"
                            },
                            {
                              "id": 2,
                              "postId": 11,
                              "authorId": 3,
                              "authorName": "other",
                              "parentId": 1,
                              "depth": 1,
                              "content": "대댓글",
                              "deleted": false,
                              "createdAt": "2026-04-08T11:21:00",
                              "updatedAt": "2026-04-08T11:21:00"
                            }
                          ],
                          "message": "댓글 목록 조회 성공"
                        }
                        """)))
    })
    ResponseEntity<RsData<List<CommentInfoResponse>>> getComments(
            @Parameter(description = "게시글 ID", example = "11") Long postId);

    @Operation(summary = "댓글 작성", description = "인증된 사용자가 댓글 또는 대댓글(depth=1)을 작성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "작성 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples =
                                @ExampleObject(
                                        value =
                                                """
                        {
                          "data": {
                            "id": 3,
                            "depth": 0,
                            "deleted": false
                          },
                          "message": "댓글 작성 성공"
                        }
                        """))),
        @ApiResponse(
                responseCode = "400",
                description = "검증 실패 또는 depth 정책 위반",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"대댓글의 대댓글은 작성할 수 없습니다.\"}"))),
        @ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"로그인이 필요합니다.\"}")))
    })
    ResponseEntity<RsData<CommentInfoResponse>> createComment(
            @Parameter(hidden = true) AuthenticatedMember authenticatedMember,
            @Parameter(description = "게시글 ID", example = "11") Long postId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "댓글 본문 및 부모 댓글 ID(대댓글일 때)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    value =
                                                            """
                                    {
                                      "parentCommentId": null,
                                      "content": "좋은 글 감사합니다!"
                                    }
                                    """)))
                    @Valid CreateCommentRequest request);

    @Operation(summary = "댓글 수정", description = "작성자 본인 또는 관리자 권한으로 댓글을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
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
                            "id": 3,
                            "content": "수정된 댓글"
                          },
                          "message": "댓글 수정 성공"
                        }
                        """))),
        @ApiResponse(
                responseCode = "403",
                description = "권한 없음",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"본인 댓글만 수정할 수 있습니다.\"}"))),
        @ApiResponse(
                responseCode = "400",
                description = "삭제된 댓글 수정 시도",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"삭제된 댓글은 수정할 수 없습니다.\"}")))
    })
    ResponseEntity<RsData<CommentInfoResponse>> updateComment(
            @Parameter(hidden = true) AuthenticatedMember authenticatedMember,
            @Parameter(description = "게시글 ID", example = "11") Long postId,
            @Parameter(description = "댓글 ID", example = "3") Long commentId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            description = "수정할 댓글 본문",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples =
                                            @ExampleObject(
                                                    value =
                                                            """
                                    {
                                      "content": "수정된 댓글"
                                    }
                                    """)))
                    @Valid UpdateCommentRequest request);

    @Operation(summary = "댓글 삭제(소프트 삭제)", description = "댓글 내용을 '삭제된 댓글입니다.'로 변경하고 수정 불가 상태로 전환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "삭제 성공",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"댓글 삭제 성공\"}"))),
        @ApiResponse(
                responseCode = "403",
                description = "권한 없음",
                content = @Content(
                        mediaType = "application/json",
                        examples = @ExampleObject(value = "{\"data\":null,\"message\":\"본인 댓글만 삭제할 수 있습니다.\"}")))
    })
    ResponseEntity<RsData<Void>> deleteComment(
            @Parameter(hidden = true) AuthenticatedMember authenticatedMember,
            @Parameter(description = "게시글 ID", example = "11") Long postId,
            @Parameter(description = "댓글 ID", example = "3") Long commentId);
}
