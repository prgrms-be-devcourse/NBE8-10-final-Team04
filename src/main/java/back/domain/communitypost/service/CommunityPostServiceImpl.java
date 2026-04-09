package back.domain.communitypost.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import back.domain.communitypost.dto.AdminChangeCommunityPostStatusRequest;
import back.domain.communitypost.dto.AdminGenerateCommunityPostRequest;
import back.domain.communitypost.dto.AdminUpdateCommunityPostRequest;
import back.domain.communitypost.dto.CommunityPostInfoResponse;
import back.domain.communitypost.dto.PageCommunityPostResponse;
import back.domain.communitypost.entity.CommunityPost;
import back.domain.communitypost.entity.CommunityPostStatus;
import back.domain.communitypost.entity.CommunityPostType;
import back.domain.communitypost.repository.CommunityPostRepository;
import back.domain.info.dto.response.BenchmarkMetricView;
import back.domain.info.dto.response.UpdateRequestCommunityView;
import back.domain.info.service.InfoCommunityReadService;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "스프링 DI로 주입되는 빈 참조이며 의도된 패턴이다.")
@Transactional(readOnly = true)
public class CommunityPostServiceImpl implements CommunityPostService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final long NO_VENDOR_PLACEHOLDER = 0L;

    private final CommunityPostRepository communityPostRepository;
    private final MemberRepository memberRepository;
    private final InfoCommunityReadService infoCommunityReadService;

    @Override
    @Transactional
    public CommunityPostInfoResponse generatePost(long adminId, AdminGenerateCommunityPostRequest request) {
        Member admin = getAdminOrThrow(adminId);
        Long resolvedVendorId = resolveVendorIdByType(request.type(), request.vendorId());
        validateDuplicateDailyPost(request.type(), request.targetDate(), resolvedVendorId);

        GeneratedPostContent generatedContent = generateContent(request.type(), request.targetDate(), resolvedVendorId);
        CommunityPost post = CommunityPost.createPendingReview(
                request.type(),
                generatedContent.title(),
                generatedContent.summary(),
                generatedContent.sourceUrl(),
                admin,
                request.targetDate(),
                resolvedVendorId);
        CommunityPostInfoResponse response = savePostWithDuplicateGuard(post);
        if (!generatedContent.approveUpdateRequestIds().isEmpty()) {
            infoCommunityReadService.approveUpdateRequests(generatedContent.approveUpdateRequestIds());
        }
        return response;
    }

    @Override
    @Transactional
    public CommunityPostInfoResponse updatePost(long adminId, long postId, AdminUpdateCommunityPostRequest request) {
        validateAdminMember(adminId);
        CommunityPost post = getPostOrThrow(postId);
        post.updateContent(request.title(), request.summary(), request.sourceUrl());
        return CommunityPostInfoResponse.from(post);
    }

    @Override
    @Transactional
    public void deletePost(long adminId, long postId) {
        validateAdminMember(adminId);
        CommunityPost post = getPostOrThrow(postId);
        post.changeStatus(CommunityPostStatus.HIDDEN);
    }

    @Override
    @Transactional
    public CommunityPostInfoResponse changeStatus(
            long adminId, long postId, AdminChangeCommunityPostStatusRequest request) {
        validateAdminMember(adminId);
        CommunityPost post = getPostOrThrow(postId);
        post.changeStatus(request.status());
        return CommunityPostInfoResponse.from(post);
    }

    @Override
    public PageCommunityPostResponse getPublicPosts(Pageable pageable) {
        Page<CommunityPostInfoResponse> page = communityPostRepository
                .findAllByStatus(CommunityPostStatus.PUBLISHED, pageable)
                .map(CommunityPostInfoResponse::from);
        return PageCommunityPostResponse.from(page);
    }

    @Override
    public CommunityPostInfoResponse getPublicPost(long postId) {
        return CommunityPostInfoResponse.from(getPublishedPostOrThrow(postId));
    }

    @Override
    public PageCommunityPostResponse getAdminPosts(CommunityPostStatus status, Pageable pageable) {
        Page<CommunityPostInfoResponse> page = (status == null
                        ? communityPostRepository.findAll(pageable)
                        : communityPostRepository.findAllByStatus(status, pageable))
                .map(CommunityPostInfoResponse::from);
        return PageCommunityPostResponse.from(page);
    }

    @Override
    public CommunityPost getPublishedPostOrThrow(long postId) {
        return communityPostRepository
                .findByIdAndStatus(postId, CommunityPostStatus.PUBLISHED)
                .orElseThrow(() -> new ServiceException(
                        CommonErrorCode.NOT_FOUND,
                        "[CommunityPostServiceImpl#getPublishedPostOrThrow] published post not found",
                        "게시글을 찾을 수 없습니다."));
    }

    @Override
    public CommunityPost getPostOrThrow(long postId) {
        return communityPostRepository
                .findById(postId)
                .orElseThrow(() -> new ServiceException(
                        CommonErrorCode.NOT_FOUND,
                        "[CommunityPostServiceImpl#getPostOrThrow] post not found",
                        "게시글을 찾을 수 없습니다."));
    }

    private GeneratedPostContent generateContent(CommunityPostType type, LocalDate targetDate, Long vendorId) {
        return switch (type) {
            case MODEL_INFO -> generateModelInfoContent(targetDate, vendorId);
            case PERFORMANCE_COMPARISON -> generatePerformanceComparisonContent(targetDate);
        };
    }

    private GeneratedPostContent generateModelInfoContent(LocalDate targetDate, Long vendorId) {
        List<UpdateRequestCommunityView> updateRequests =
                infoCommunityReadService.getPendingUpdateRequestsByDateAndVendor(targetDate, vendorId);
        if (updateRequests.isEmpty()) {
            throw new ServiceException(
                    CommonErrorCode.NOT_FOUND,
                    "[CommunityPostServiceImpl#generateModelInfoContent] no update request source data",
                    "해당 벤더/날짜의 업데이트 원천 데이터가 없습니다.");
        }

        String formattedDate = DATE_FORMATTER.format(targetDate);
        String vendorName = updateRequests.getFirst().vendorName();
        String title = "%s %s 모델 정보 업데이트".formatted(formattedDate, vendorName);
        String summary = resolveModelInfoSummary(formattedDate, vendorName, updateRequests);
        String sourceUrl = updateRequests.stream()
                .map(UpdateRequestCommunityView::sourceUrl)
                .filter(this::hasText)
                .findFirst()
                .orElse(null);

        List<Long> requestIdsToApprove = new ArrayList<>();
        for (UpdateRequestCommunityView updateRequest : updateRequests) {
            if (updateRequest.id() != null) {
                requestIdsToApprove.add(updateRequest.id());
            }
        }
        return new GeneratedPostContent(title, summary, sourceUrl, requestIdsToApprove);
    }

    private String resolveModelInfoSummary(
            String formattedDate, String vendorName, List<UpdateRequestCommunityView> updateRequests) {
        return updateRequests.stream()
                .map(UpdateRequestCommunityView::summary)
                .filter(this::hasText)
                .findFirst()
                .orElse("%s 기준 %s 업데이트 요청 %d건".formatted(formattedDate, vendorName, updateRequests.size()));
    }

    private GeneratedPostContent generatePerformanceComparisonContent(LocalDate targetDate) {
        List<BenchmarkMetricView> benchmarkViews = infoCommunityReadService.getPerformanceMetricsByDate(targetDate);
        if (benchmarkViews.isEmpty()) {
            throw new ServiceException(
                    CommonErrorCode.NOT_FOUND,
                    "[CommunityPostServiceImpl#generatePerformanceComparisonContent] no benchmark source data",
                    "해당 날짜의 성능 비교 원천 데이터가 없습니다.");
        }

        Map<String, BenchmarkMetricView> latestByModelAndMetric = new LinkedHashMap<>();
        for (BenchmarkMetricView metricView : benchmarkViews) {
            String key = metricView.modelApiId() + "|" + metricView.metricType().name();
            BenchmarkMetricView existing = latestByModelAndMetric.get(key);
            if (existing == null || metricView.measuredAt().isAfter(existing.measuredAt())) {
                latestByModelAndMetric.put(key, metricView);
            }
        }

        List<BenchmarkMetricView> latestMetrics = latestByModelAndMetric.values().stream()
                .sorted((left, right) -> {
                    int byModel = left.modelApiId().compareTo(right.modelApiId());
                    if (byModel != 0) {
                        return byModel;
                    }
                    return left.metricType().name().compareTo(right.metricType().name());
                })
                .toList();

        String formattedDate = DATE_FORMATTER.format(targetDate);
        String title = "%s 성능 비교 리포트".formatted(formattedDate);
        String summary = "%s 기준 성능 지표 %d건".formatted(formattedDate, latestMetrics.size());

        String sourceUrl = "internal://benchmarks/%s".formatted(targetDate);
        return new GeneratedPostContent(title, summary, sourceUrl, List.of());
    }

    private void validateDuplicateDailyPost(CommunityPostType type, LocalDate targetDate, Long vendorId) {
        boolean duplicated = type == CommunityPostType.MODEL_INFO
                ? communityPostRepository.existsByPostTypeAndTargetDateAndVendorId(type, targetDate, vendorId)
                : communityPostRepository.existsByPostTypeAndTargetDate(type, targetDate);

        if (duplicated) {
            throw new ServiceException(
                    CommonErrorCode.CONFLICT,
                    "[CommunityPostServiceImpl#validateDuplicateDailyPost] duplicated type/date post generation",
                    "해당 날짜와 타입의 게시글이 이미 존재합니다.");
        }
    }

    private Long resolveVendorIdByType(CommunityPostType type, Long vendorId) {
        if (type == CommunityPostType.MODEL_INFO) {
            if (vendorId == null || vendorId <= 0) {
                throw new ServiceException(
                        CommonErrorCode.BAD_REQUEST,
                        "[CommunityPostServiceImpl#resolveVendorIdByType] vendorId is required for MODEL_INFO",
                        "MODEL_INFO 게시글 생성 시 vendorId는 필수입니다.");
            }
            return vendorId;
        }
        return NO_VENDOR_PLACEHOLDER;
    }

    private CommunityPostInfoResponse savePostWithDuplicateGuard(CommunityPost post) {
        try {
            return CommunityPostInfoResponse.from(communityPostRepository.save(post));
        } catch (DataIntegrityViolationException ex) {
            throw new ServiceException(
                    CommonErrorCode.CONFLICT,
                    "[CommunityPostServiceImpl#savePostWithDuplicateGuard] duplicated post by unique constraint",
                    "해당 날짜와 타입의 게시글이 이미 존재합니다.");
        }
    }

    private void validateAdminMember(long memberId) {
        getAdminOrThrow(memberId);
    }

    private Member getAdminOrThrow(long memberId) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ServiceException(
                        CommonErrorCode.NOT_FOUND,
                        "[CommunityPostServiceImpl#getAdminOrThrow] member not found",
                        "회원이 존재하지 않습니다."));

        if (!member.isAdmin()) {
            throw new ServiceException(
                    CommonErrorCode.FORBIDDEN,
                    "[CommunityPostServiceImpl#getAdminOrThrow] admin permission required",
                    "관리자 권한이 필요합니다.");
        }
        return member;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record GeneratedPostContent(
            String title,
            String summary,
            String sourceUrl,
            List<Long> approveUpdateRequestIds) {}
}
