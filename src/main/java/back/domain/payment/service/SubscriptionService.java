package back.domain.payment.service;

import back.domain.member.entity.Member;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionStatus;
import back.domain.payment.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private SubscriptionRepository subscriptionRepository;

//    구독 상태 조회
    public SubscriptionStatus getStatus(Long memberId) {
        return subscriptionRepository.findByMemberId(memberId)
                .map(Subscription::getStatus)
                .orElse(SubscriptionStatus.INACTIVE);
    }
}
