package back.domain.communitypost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
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
import back.domain.info.dto.response.UpdateRequestCommunityView;
import back.domain.info.enums.MetricType;
import back.domain.info.service.InfoCommunityReadService;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.global.exception.CommonErrorCode;
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
        when(infoCommunityReadService.getPendingUpdateRequestsByDateAndVendor(targetDate, vendorId))
                .thenReturn(List.of(new UpdateRequestCommunityView(
                        101L,
                        "OpenAI",
                        "GPT-5.4",
                        "https://news.example.com/openai",
                        "RSS",
                        "GPT-5.4 업데이트",
                        "원문 본문",
                        targetDate.atStartOfDay())));
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
        assertThat(postCaptor.getValue().getSourceUrl()).isEqualTo("https://news.example.com/openai");
        verify(infoCommunityReadService).approveUpdateRequests(eq(List.of(101L)));
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
                "https://example.com/post",
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
        assertThat(response.sourceUrl()).isEqualTo("internal://benchmarks/2026-04-08");
        verify(infoCommunityReadService, never()).approveUpdateRequests(any());
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
        when(infoCommunityReadService.getPendingUpdateRequestsByDateAndVendor(targetDate, vendorId))
                .thenReturn(List.of(new UpdateRequestCommunityView(
                        101L,
                        "OpenAI",
                        "GPT-5.4",
                        "https://news.example.com/openai",
                        "RSS",
                        "GPT-5.4 업데이트",
                        "원문 본문",
                        targetDate.atStartOfDay())));
        when(communityPostRepository.save(any(CommunityPost.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint",
                        new SQLException("duplicate key value violates unique constraint", "23505")));

        assertThatThrownBy(() -> communityPostService.generatePost(
                        adminId, new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, targetDate, vendorId)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.CONFLICT);
                    assertThat(serviceException.getClientMessage()).isEqualTo("해당 날짜와 타입의 게시글이 이미 존재합니다.");
                });
        verify(infoCommunityReadService, never()).approveUpdateRequests(any());
    }

    @Test
    @DisplayName("DB NOT NULL 제약 충돌 시 BAD_REQUEST 예외를 반환한다")
    void generatePost_whenNotNullConstraintViolation_thenThrowBadRequest() {
        long adminId = 1L;
        long vendorId = 10L;
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        Member admin = Member.createAdmin("sub-admin", "admin@example.com", "Admin");

        when(memberRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));
        when(communityPostRepository.existsByPostTypeAndTargetDateAndVendorId(
                        CommunityPostType.MODEL_INFO, targetDate, vendorId))
                .thenReturn(false);
        when(infoCommunityReadService.getPendingUpdateRequestsByDateAndVendor(targetDate, vendorId))
                .thenReturn(List.of(new UpdateRequestCommunityView(
                        101L,
                        "OpenAI",
                        "GPT-5.4",
                        "https://news.example.com/openai",
                        "RSS",
                        "GPT-5.4 업데이트",
                        "원문 본문",
                        targetDate.atStartOfDay())));
        when(communityPostRepository.save(any(CommunityPost.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "null value in column \"source_url\" violates not-null constraint",
                        new SQLException("null value in column \"source_url\" violates not-null constraint", "23502")));

        assertThatThrownBy(() -> communityPostService.generatePost(
                        adminId, new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, targetDate, vendorId)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST);
                    assertThat(serviceException.getClientMessage()).isEqualTo("게시글 생성에 필요한 필수 데이터가 누락되었습니다.");
                });
        verify(infoCommunityReadService, never()).approveUpdateRequests(any());
    }

    @Test
    @DisplayName("MODEL_INFO 생성 시 sourceUrl이 비어 있으면 NOT_FOUND 예외를 반환한다")
    void generatePost_whenSourceUrlMissing_thenThrowNotFound() {
        long adminId = 1L;
        long vendorId = 10L;
        LocalDate targetDate = LocalDate.of(2026, 4, 10);
        Member admin = Member.createAdmin("sub-admin", "admin@example.com", "Admin");

        when(memberRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));
        when(communityPostRepository.existsByPostTypeAndTargetDateAndVendorId(
                        CommunityPostType.MODEL_INFO, targetDate, vendorId))
                .thenReturn(false);
        when(infoCommunityReadService.getPendingUpdateRequestsByDateAndVendor(targetDate, vendorId))
                .thenReturn(List.of(new UpdateRequestCommunityView(
                        101L,
                        "OpenAI",
                        "GPT-5.4",
                        "   ",
                        "RSS",
                        "GPT-5.4 업데이트",
                        "원문 본문",
                        targetDate.atStartOfDay())));

        assertThatThrownBy(() -> communityPostService.generatePost(
                        adminId, new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, targetDate, vendorId)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.NOT_FOUND);
                    assertThat(serviceException.getClientMessage())
                            .isEqualTo("해당 벤더/날짜의 업데이트 원천 데이터에 sourceUrl이 없습니다.");
                });
        verify(communityPostRepository, never()).save(any());
        verify(infoCommunityReadService, never()).approveUpdateRequests(any());
    }

    @Test
    @DisplayName("DB 무결성 오류가 중복 제약이 아니면 INTERNAL_SERVER_ERROR 예외를 반환한다")
    void generatePost_whenUnexpectedIntegrityViolation_thenThrowInternalServerError() {
        long adminId = 1L;
        long vendorId = 10L;
        LocalDate targetDate = LocalDate.of(2026, 4, 10);
        Member admin = Member.createAdmin("sub-admin", "admin@example.com", "Admin");

        when(memberRepository.findById(adminId)).thenReturn(java.util.Optional.of(admin));
        when(communityPostRepository.existsByPostTypeAndTargetDateAndVendorId(
                        CommunityPostType.MODEL_INFO, targetDate, vendorId))
                .thenReturn(false);
        when(infoCommunityReadService.getPendingUpdateRequestsByDateAndVendor(targetDate, vendorId))
                .thenReturn(List.of(new UpdateRequestCommunityView(
                        101L,
                        "OpenAI",
                        "GPT-5.4",
                        "https://news.example.com/openai",
                        "RSS",
                        "GPT-5.4 업데이트",
                        "원문 본문",
                        targetDate.atStartOfDay())));
        when(communityPostRepository.save(any(CommunityPost.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "unexpected integrity violation",
                        new SQLException("unexpected integrity violation", "XX000")));

        assertThatThrownBy(() -> communityPostService.generatePost(
                        adminId, new AdminGenerateCommunityPostRequest(CommunityPostType.MODEL_INFO, targetDate, vendorId)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException serviceException = (ServiceException) ex;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR);
                    assertThat(serviceException.getClientMessage()).isEqualTo("게시글 생성 중 데이터 무결성 오류가 발생했습니다.");
                });
        verify(infoCommunityReadService, never()).approveUpdateRequests(any());
    }
}
