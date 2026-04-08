package back.domain.payment.service;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.domain.payment.entity.Subscription;
import back.domain.payment.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UsageService {

    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public int getRemainingUsage(Long memberId) {
        Member member = getMember(memberId);
        member.resetFreeUsageIfNeeded();
        return member.getRemainingUsage();
    }

    @Transactional
    public int useOnce(Long memberId) {
        Member member = getMember(memberId);
        member.resetFreeUsageIfNeeded();
        member.increaseFreeUsageCount();
        return member.getRemainingUsage();
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));
    }

    //    검증 + 차감 메서드
    @Transactional
    public void validateAndConsume(Long memberId) {
        Member member = getMember(memberId);
        member.resetFreeUsageIfNeeded();

        member.updateLastUsageAt();

        // 활성 구독자면 차감 없이 통과
        boolean isSubscribed = subscriptionRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .map(Subscription::isActive)
                .orElse(false);

        if (isSubscribed) return;

        // 무료 사용자 횟수 차감 (초과시 예외)
        member.increaseFreeUsageCount();
    }

    public int getUsageCountForToday(Long memberId) {
        Member member = getMember(memberId);
        // 오늘 무료로 몇 번 썼는지 반환 (환불 판단용)
        return member.getFreeUsageCount();
    }
}