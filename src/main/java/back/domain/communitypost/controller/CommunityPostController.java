package back.domain.communitypost.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import back.domain.communitypost.dto.CommunityPostInfoResponse;
import back.domain.communitypost.dto.PageCommunityPostResponse;
import back.domain.communitypost.service.CommunityPostService;
import back.global.response.RsData;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/posts")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "스프링 DI로 주입되는 빈 참조이며 의도된 패턴이다.")
public class CommunityPostController {

    private final CommunityPostService communityPostService;

    @GetMapping
    public ResponseEntity<RsData<PageCommunityPostResponse>> getPublicPosts(
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new RsData<>(communityPostService.getPublicPosts(pageable), "게시글 목록 조회 성공"));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<RsData<CommunityPostInfoResponse>> getPublicPost(@PathVariable Long postId) {
        return ResponseEntity.ok(new RsData<>(communityPostService.getPublicPost(postId), "게시글 조회 성공"));
    }
}
