package back.domain.communitypost.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import back.domain.communitypost.dto.AdminChangeCommunityPostStatusRequest;
import back.domain.communitypost.dto.AdminGenerateCommunityPostRequest;
import back.domain.communitypost.dto.AdminUpdateCommunityPostRequest;
import back.domain.communitypost.dto.CommunityPostInfoResponse;
import back.domain.communitypost.dto.PageCommunityPostResponse;
import back.domain.communitypost.entity.CommunityPostStatus;
import back.domain.communitypost.service.CommunityPostService;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.response.RsData;
import back.global.security.AuthenticatedMember;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/admin/community/posts")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "스프링 DI로 주입되는 빈 참조이며 의도된 패턴이다.")
public class AdminCommunityPostController {

    private final CommunityPostService communityPostService;

    @GetMapping
    public ResponseEntity<RsData<PageCommunityPostResponse>> getAdminPosts(
            @RequestParam(required = false) CommunityPostStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageCommunityPostResponse response = communityPostService.getAdminPosts(status, pageable);
        return ResponseEntity.ok(new RsData<>(response, "관리자 게시글 목록 조회 성공"));
    }

    @PostMapping("/generate")
    public ResponseEntity<RsData<CommunityPostInfoResponse>> generatePost(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody AdminGenerateCommunityPostRequest request) {
        long adminId = resolveAuthenticatedMemberId(authenticatedMember);
        CommunityPostInfoResponse response = communityPostService.generatePost(adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RsData<>(response, "게시글 생성 성공"));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<RsData<CommunityPostInfoResponse>> updatePost(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable Long postId,
            @Valid @RequestBody AdminUpdateCommunityPostRequest request) {
        long adminId = resolveAuthenticatedMemberId(authenticatedMember);
        return ResponseEntity.ok(new RsData<>(communityPostService.updatePost(adminId, postId, request), "게시글 수정 성공"));
    }

    @PatchMapping("/{postId}/status")
    public ResponseEntity<RsData<CommunityPostInfoResponse>> changeStatus(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable Long postId,
            @Valid @RequestBody AdminChangeCommunityPostStatusRequest request) {
        long adminId = resolveAuthenticatedMemberId(authenticatedMember);
        CommunityPostInfoResponse response = communityPostService.changeStatus(adminId, postId, request);
        return ResponseEntity.ok(new RsData<>(response, "게시글 상태 변경 성공"));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<RsData<Void>> deletePost(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember, @PathVariable Long postId) {
        long adminId = resolveAuthenticatedMemberId(authenticatedMember);
        communityPostService.deletePost(adminId, postId);
        return ResponseEntity.ok(new RsData<>("게시글 삭제 성공"));
    }

    private long resolveAuthenticatedMemberId(AuthenticatedMember authenticatedMember) {
        if (authenticatedMember == null) {
            throw new ServiceException(
                    CommonErrorCode.UNAUTHORIZED,
                    "[AdminCommunityPostController#resolveAuthenticatedMemberId] authenticated member is missing",
                    CommonErrorCode.UNAUTHORIZED.defaultMessage());
        }

        return authenticatedMember.memberId();
    }
}
