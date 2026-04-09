package back.domain.communitypost.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import back.domain.communitypost.dto.AdminChangeCommunityPostStatusRequest;
import back.domain.communitypost.dto.AdminGenerateCommunityPostRequest;
import back.domain.communitypost.dto.AdminUpdateCommunityPostRequest;
import back.domain.communitypost.dto.CommunityPostInfoResponse;
import back.domain.communitypost.dto.PageCommunityPostResponse;
import back.domain.communitypost.entity.CommunityPostStatus;
import back.domain.communitypost.entity.CommunityPostType;
import back.domain.communitypost.service.CommunityPostService;
import back.global.exception.ServiceException;
import back.global.response.RsData;
import back.global.security.AuthenticatedMember;

class AdminCommunityPostControllerTest {

    private CommunityPostService communityPostService;
    private AdminCommunityPostController controller;

    @BeforeEach
    void setUp() {
        communityPostService = mock(CommunityPostService.class);
        controller = new AdminCommunityPostController(communityPostService);
    }

    @Test
    void getAdminPosts_returnsServiceResponse() {
        PageCommunityPostResponse page = new PageCommunityPostResponse(List.of(), 0, 0, 0, 20);
        when(communityPostService.getAdminPosts(CommunityPostStatus.PENDING_REVIEW, PageRequest.of(0, 20)))
                .thenReturn(page);

        ResponseEntity<RsData<PageCommunityPostResponse>> response =
                controller.getAdminPosts(CommunityPostStatus.PENDING_REVIEW, PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(page);
        assertThat(response.getBody().message()).isEqualTo("관리자 게시글 목록 조회 성공");
    }

    @Test
    void generatePost_returnsCreated() {
        AuthenticatedMember admin = new AuthenticatedMember(1L, "ADMIN");
        AdminGenerateCommunityPostRequest request =
                new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, LocalDate.of(2026, 4, 8), 10L);
        CommunityPostInfoResponse post = samplePost();
        when(communityPostService.generatePost(1L, request)).thenReturn(post);

        ResponseEntity<RsData<CommunityPostInfoResponse>> response = controller.generatePost(admin, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(post);
        assertThat(response.getBody().message()).isEqualTo("게시글 생성 성공");
    }

    @Test
    void updatePost_returnsOk() {
        AuthenticatedMember admin = new AuthenticatedMember(1L, "ADMIN");
        AdminUpdateCommunityPostRequest request =
                new AdminUpdateCommunityPostRequest("수정 제목", "수정 요약", "https://news.example.com/openai/gpt-5-4");
        CommunityPostInfoResponse post = samplePost();
        when(communityPostService.updatePost(1L, 5L, request)).thenReturn(post);

        ResponseEntity<RsData<CommunityPostInfoResponse>> response = controller.updatePost(admin, 5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("게시글 수정 성공");
    }

    @Test
    void changeStatus_returnsOk() {
        AuthenticatedMember admin = new AuthenticatedMember(1L, "ADMIN");
        AdminChangeCommunityPostStatusRequest request =
                new AdminChangeCommunityPostStatusRequest(CommunityPostStatus.PUBLISHED);
        CommunityPostInfoResponse post = samplePost();
        when(communityPostService.changeStatus(1L, 5L, request)).thenReturn(post);

        ResponseEntity<RsData<CommunityPostInfoResponse>> response = controller.changeStatus(admin, 5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("게시글 상태 변경 성공");
    }

    @Test
    void deletePost_returnsOk() {
        AuthenticatedMember admin = new AuthenticatedMember(1L, "ADMIN");

        ResponseEntity<RsData<Void>> response = controller.deletePost(admin, 5L);

        verify(communityPostService).deletePost(1L, 5L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("게시글 삭제 성공");
    }

    @Test
    void generatePost_withoutAuthenticatedMember_throwsUnauthorized() {
        AdminGenerateCommunityPostRequest request =
                new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, LocalDate.of(2026, 4, 8), 10L);

        assertThatThrownBy(() -> controller.generatePost(null, request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("resolveAuthenticatedMemberId");
    }

    private CommunityPostInfoResponse samplePost() {
        return new CommunityPostInfoResponse(
                5L,
                CommunityPostType.MODEL_INFO,
                "제목",
                "요약",
                "https://news.example.com/openai/gpt-5-4",
                CommunityPostStatus.PENDING_REVIEW,
                LocalDate.of(2026, 4, 8),
                10L,
                null,
                null,
                null);
    }
}
