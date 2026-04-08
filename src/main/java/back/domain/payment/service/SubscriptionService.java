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

    // 구독 취소 (한달뒤)
    @Transactional
    public Subscription cancelSubscription(Long memberId) {
        Subscription subscription = subscriptionRepository.findByMemberId(memberId)
                .filter(Subscription::isActive) // 활성화된 구독만 필터링
                .orElseThrow(() -> new IllegalArgumentException("해지할 수 있는 활성 구독권이 없습니다."));

        subscription.cancel();
        return subscription;
    }

    // 당일결제, 사용기록 없으면 해지 가능(당일만)
    @Transactional
    public boolean cancelWithRefund(Long memberId) {
        Optional<Payment> todayPaymentOpt = paymentRepository.findTodayPaymentByMemberId(memberId);

        // Member 엔티티에 새로 만든 lastUsageAt 활용 (아까 만든 로직)
        Member member = memberRepository.findById(memberId).orElseThrow();
        LocalDateTime lastUsedAt = member.getLastUsageAt();

        if (todayPaymentOpt.isPresent() && (lastUsedAt == null || !lastUsedAt.isAfter(todayPaymentOpt.get().getCreatedAt()))) {
            Payment payment = todayPaymentOpt.get();

            // 1. 실제 환불 로직 실행 (토스 API 호출 등)
            processRefund(payment);

            // 2. [핵심] 결제 내역에서 구독 참조를 끊어줍니다.
            // payment 엔티티에 setSubscription(null) 같은 메서드가 있다면 호출하세요.
            // 만약 필드가 연관관계 매핑이 되어있다면 아래처럼 끊어줘야 삭제가 가능합니다.
            payment.unlinkSubscription();
            paymentRepository.save(payment); // 변경 사항 반영

            // 3. 이제 안전하게 삭제 가능!
            subscriptionRepository.deleteByMemberId(memberId);
            return true;
        } else {
            this.cancelSubscription(memberId);
            return false;
        }
    }

    private void processRefund(Payment payment) {
        // 실제로는 여기서 토스 환불 API를 호출해야 합니다.
        // 지금은 로그만 찍거나 간단한 로직만 넣어두세요.
        System.out.println("결제건 환불 처리 중: " + payment.getOrderId());

        // 결제 상태를 환불(CANCELED)로 변경하는 로직 등
        payment.markAsCanceled();
    }

    public Subscription findByMemberId(Long memberId) {
        return subscriptionRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));
    }
}
