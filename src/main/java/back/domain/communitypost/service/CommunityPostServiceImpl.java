package back.domain.communitypost.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import back.domain.info.dto.response.ModelInfoFamilyView;
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
                generatedContent.body(),
                admin,
                request.targetDate(),
                resolvedVendorId);

        return CommunityPostInfoResponse.from(communityPostRepository.save(post));
    }

    @Override
    @Transactional
    public CommunityPostInfoResponse updatePost(long adminId, long postId, AdminUpdateCommunityPostRequest request) {
        validateAdminMember(adminId);
        CommunityPost post = getPostOrThrow(postId);
        post.updateContent(request.title(), request.summary(), request.body());
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
        List<ModelInfoFamilyView> families = infoCommunityReadService.getModelInfoByDateAndVendor(targetDate, vendorId);
        if (families.isEmpty()) {
            throw new ServiceException(
                    CommonErrorCode.NOT_FOUND,
                    "[CommunityPostServiceImpl#generateModelInfoContent] no model info source data",
                    "해당 벤더/날짜의 모델 정보 원천 데이터가 없습니다.");
        }

        String formattedDate = DATE_FORMATTER.format(targetDate);
        String vendorName = families.getFirst().vendorName();
        String title = "%s %s 모델 정보 업데이트".formatted(formattedDate, vendorName);
        String summary = "%s 기준 %s 모델 패밀리 변경 %d건".formatted(formattedDate, vendorName, families.size());

        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("# ").append(formattedDate).append(" ").append(vendorName).append(" 모델 정보 업데이트\n\n");
        bodyBuilder.append("- 벤더: ").append(vendorName).append("\n");
        bodyBuilder.append("- 총 변경 패밀리 수: ").append(families.size()).append("\n\n");

        for (ModelInfoFamilyView family : families) {
            bodyBuilder.append("- 패밀리: ").append(family.familyName()).append("\n");
            bodyBuilder
                    .append("  - 설명: ")
                    .append(defaultText(family.commonDescription()))
                    .append("\n");
            bodyBuilder
                    .append("  - 입력 타입: ")
                    .append(joinArray(family.inputTypes()))
                    .append("\n");
            bodyBuilder
                    .append("  - 출력 타입: ")
                    .append(joinArray(family.outputTypes()))
                    .append("\n\n");
        }

        return new GeneratedPostContent(title, summary, bodyBuilder.toString());
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

        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("# ").append(formattedDate).append(" 성능 비교 리포트\n\n");
        bodyBuilder.append("| 모델 | 지표 | 값 | 단위 | 측정 시각 |\n");
        bodyBuilder.append("| --- | --- | ---: | --- | --- |\n");
        for (BenchmarkMetricView metric : latestMetrics) {
            bodyBuilder
                    .append("| ")
                    .append(metric.modelApiId())
                    .append(" | ")
                    .append(metric.metricType().name())
                    .append(" | ")
                    .append(metric.metricValue())
                    .append(" | ")
                    .append(defaultText(metric.unit()))
                    .append(" | ")
                    .append(metric.measuredAt())
                    .append(" |\n");
        }

        return new GeneratedPostContent(title, summary, bodyBuilder.toString());
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
        return null;
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

    private String defaultText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private String joinArray(String[] values) {
        if (values == null || values.length == 0) {
            return "-";
        }

        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim());
            }
        }

        if (normalized.isEmpty()) {
            return "-";
        }

        return String.join(", ", normalized);
    }

    private record GeneratedPostContent(String title, String summary, String body) {}
}
