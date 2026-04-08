package back.domain.payment.service;

import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.domain.payment.dto.response.SubscriptionStatusResponse;
import back.domain.payment.entity.Payment;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionStatus;
import back.domain.payment.repository.PaymentRepository;
import back.domain.payment.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("EI_EXPOSE_REP2")
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

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

    @Transactional
    public Subscription cancelSubscription(Long memberId) {
        Subscription subscription = subscriptionRepository.findByMemberId(memberId)
                .filter(Subscription::isActive)
                .orElseThrow(() -> new IllegalArgumentException("해지할 수 있는 활성 구독권이 없습니다."));

        subscription.cancel();
        return subscription;
    }

    @Transactional
    public boolean cancelWithRefund(Long memberId) {
        Optional<Payment> todayPaymentOpt = paymentRepository.findTodayPaymentByMemberId(memberId);

        Member member = memberRepository.findById(memberId).orElseThrow();
        LocalDateTime lastUsedAt = member.getLastUsageAt();

        if (todayPaymentOpt.isPresent() && (lastUsedAt == null || !lastUsedAt.isAfter(todayPaymentOpt.get().getCreatedAt()))) {
            Payment payment = todayPaymentOpt.get();

            processRefund(payment);

            payment.unlinkSubscription();
            paymentRepository.save(payment);

            subscriptionRepository.deleteByMemberId(memberId);
            return true;
        } else {
            this.cancelSubscription(memberId);
            return false;
        }
    }

    private void processRefund(Payment payment) {
        System.out.println("결제건 환불 처리 중: " + payment.getOrderId());
        payment.markAsCanceled();
    }

    public Subscription findByMemberId(Long memberId) {
        return subscriptionRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));
    }
}