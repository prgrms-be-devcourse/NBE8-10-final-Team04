package back.domain.communitypost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import back.domain.communitypost.dto.AdminGenerateCommunityPostRequest;
import back.domain.communitypost.dto.CommunityPostInfoResponse;
import back.domain.communitypost.entity.CommunityPost;
import back.domain.communitypost.entity.CommunityPostStatus;
import back.domain.communitypost.entity.CommunityPostType;
import back.domain.communitypost.repository.CommunityPostRepository;
import back.domain.info.dto.response.BenchmarkMetricView;
import back.domain.info.dto.response.ModelInfoFamilyView;
import back.domain.info.enums.MetricType;
import back.domain.info.service.InfoCommunityReadService;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
class CommunityPostServiceImplTest {

    @Mock
    private CommunityPostRepository communityPostRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private InfoCommunityReadService infoCommunityReadService;

    private CommunityPostService communityPostService;

    @BeforeEach
    void setUp() {
        communityPostService =
                new CommunityPostServiceImpl(communityPostRepository, memberRepository, infoCommunityReadService);
    }

    @Test
    @DisplayName("관리자가 MODEL_INFO 생성 요청 시 PENDING_REVIEW 게시글을 생성한다")
    void generatePost_modelInfo_success() {
        long adminId = 1L;
        long vendorId = 10L;
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        Member admin = Member.createAdmin("sub-admin", "admin@example.com", "Admin");

        when(memberRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));
        when(communityPostRepository.existsByPostTypeAndTargetDateAndVendorId(
                        CommunityPostType.MODEL_INFO, targetDate, vendorId))
                .thenReturn(false);
        when(infoCommunityReadService.getModelInfoByDateAndVendor(targetDate, vendorId))
                .thenReturn(List.of(new ModelInfoFamilyView(
                        "OpenAI", "GPT-5.4", "최신 모델", new String[] {"text"}, new String[] {"text"})));
        when(communityPostRepository.save(any(CommunityPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CommunityPostInfoResponse response = communityPostService.generatePost(
                adminId, new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, targetDate, vendorId));

        assertThat(response.type()).isEqualTo(CommunityPostType.MODEL_INFO);
        assertThat(response.status()).isEqualTo(CommunityPostStatus.PENDING_REVIEW);
        assertThat(response.targetDate()).isEqualTo(targetDate);
        assertThat(response.vendorId()).isEqualTo(vendorId);

        ArgumentCaptor<CommunityPost> postCaptor = ArgumentCaptor.forClass(CommunityPost.class);
        verify(communityPostRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getBody()).contains("OpenAI");
    }

    @Test
    @DisplayName("같은 날짜/타입 게시글이 이미 있으면 생성을 거부한다")
    void generatePost_whenDuplicateDailyType_thenThrowConflict() {
        long adminId = 1L;
        long vendorId = 10L;
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        Member admin = Member.createAdmin("sub-admin", "admin@example.com", "Admin");

        when(memberRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));
        when(communityPostRepository.existsByPostTypeAndTargetDateAndVendorId(
                        CommunityPostType.MODEL_INFO, targetDate, vendorId))
                .thenReturn(true);

        assertThatThrownBy(() -> communityPostService.generatePost(
                        adminId, new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, targetDate, vendorId)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("validateDuplicateDailyPost");
    }

    @Test
    @DisplayName("관리자가 아니면 게시글 생성을 거부한다")
    void generatePost_whenNotAdmin_thenThrowForbidden() {
        long userId = 2L;
        long vendorId = 10L;
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        Member user = Member.createUser("sub-user", "user@example.com", "User");

        when(memberRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> communityPostService.generatePost(
                        userId, new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, targetDate, vendorId)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("getAdminOrThrow");
    }

    @Test
    @DisplayName("MODEL_INFO 생성 시 vendorId가 없으면 생성을 거부한다")
    void generatePost_modelInfo_withoutVendorId_thenThrowBadRequest() {
        long adminId = 1L;
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        Member admin = Member.createAdmin("sub-admin", "admin@example.com", "Admin");

        when(memberRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));

        assertThatThrownBy(() -> communityPostService.generatePost(
                        adminId, new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, targetDate, null)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("resolveVendorIdByType");
    }

    @Test
    @DisplayName("게시글 삭제 요청 시 하드 삭제 대신 HIDDEN 상태로 소프트 삭제한다")
    void deletePost_softDeleteByHiddenStatus() {
        long adminId = 1L;
        long postId = 100L;
        Member admin = Member.createAdmin("sub-admin", "admin@example.com", "Admin");
        CommunityPost post = CommunityPost.createPendingReview(
                CommunityPostType.MODEL_INFO,
                "제목",
                "요약",
                "본문",
                admin,
                LocalDate.of(2026, 4, 8),
                10L);

        when(memberRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));
        when(communityPostRepository.findById(postId)).thenReturn(java.util.Optional.of(post));

        communityPostService.deletePost(adminId, postId);

        assertThat(post.getStatus()).isEqualTo(CommunityPostStatus.HIDDEN);
        verify(communityPostRepository, never()).delete(any());
    }

    @Test
    @DisplayName("PERFORMANCE_COMPARISON 생성은 vendorId 없이 요청해도 생성되고 응답 vendorId는 null이다")
    void generatePost_performanceComparison_success() {
        long adminId = 1L;
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        Member admin = Member.createAdmin("sub-admin", "admin@example.com", "Admin");

        when(memberRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));
        when(communityPostRepository.existsByPostTypeAndTargetDate(CommunityPostType.PERFORMANCE_COMPARISON, targetDate))
                .thenReturn(false);
        when(infoCommunityReadService.getPerformanceMetricsByDate(targetDate))
                .thenReturn(List.of(new BenchmarkMetricView(
                        "gpt-5.4",
                        MetricType.INTELLIGENCE,
                        new BigDecimal("90.10"),
                        "score",
                        LocalDateTime.of(2026, 4, 8, 10, 0))));
        when(communityPostRepository.save(any(CommunityPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CommunityPostInfoResponse response = communityPostService.generatePost(
                adminId,
                new AdminGenerateCommunityPostRequest(CommunityPostType.PERFORMANCE_COMPARISON, targetDate, null));

        assertThat(response.type()).isEqualTo(CommunityPostType.PERFORMANCE_COMPARISON);
        assertThat(response.vendorId()).isNull();
    }

    @Test
    @DisplayName("DB 유니크 제약 충돌 시 CONFLICT 예외를 반환한다")
    void generatePost_whenUniqueConstraintViolation_thenThrowConflict() {
        long adminId = 1L;
        long vendorId = 10L;
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        Member admin = Member.createAdmin("sub-admin", "admin@example.com", "Admin");

        when(memberRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));
        when(communityPostRepository.existsByPostTypeAndTargetDateAndVendorId(
                        CommunityPostType.MODEL_INFO, targetDate, vendorId))
                .thenReturn(false);
        when(infoCommunityReadService.getModelInfoByDateAndVendor(targetDate, vendorId))
                .thenReturn(List.of(new ModelInfoFamilyView(
                        "OpenAI", "GPT-5.4", "최신 모델", new String[] {"text"}, new String[] {"text"})));
        when(communityPostRepository.save(any(CommunityPost.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> communityPostService.generatePost(
                        adminId, new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, targetDate, vendorId)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("savePostWithDuplicateGuard");
    }
}
