package back.domain.payment.service;

import back.domain.member.entity.Member;
import back.domain.payment.dto.response.SubscriptionStatusResponse;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionStatus;
import back.domain.payment.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

//    구독 상태 조회
    public SubscriptionStatusResponse getStatus(Long memberId) {
        SubscriptionStatus status = subscriptionRepository.findByMemberId(memberId)
                .map(Subscription::getStatus)
                .orElse(SubscriptionStatus.INACTIVE);

        String message = switch (status) {
            case ACTIVE -> "구독 중";
            case INACTIVE -> "구독 중이 아닙니다.";
            case CANCELED -> "구독이 해지되었습니다.";
            case PAST_DUE -> "결제 실패 상태입니다.";
        };

        return new SubscriptionStatusResponse(status, message);
    }

    // 구독 취소 (한달뒤)
    @Transactional
    public Subscription cancelSubscription(Long memberId) {
        Subscription subscription = subscriptionRepository.findByMemberId(memberId)
                .filter(Subscription::isActive) // 활성화된 구독만 필터링
                .orElseThrow(() -> new IllegalArgumentException("해지할 수 있는 활성 구독권이 없습니다."));

        subscription.cancel();
        return subscription;
    }
}
