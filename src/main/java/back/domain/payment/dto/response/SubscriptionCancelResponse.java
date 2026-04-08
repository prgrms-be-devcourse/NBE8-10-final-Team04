package back.domain.payment.dto.response;

import back.domain.payment.entity.Subscription;

public record SubscriptionCancelResponse(
        String message,
        String nextBillingAt,
        String status
) {
    public static SubscriptionCancelResponse from(Subscription subscription) {
        String formattedDate = subscription.getNextBillingAt()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));

        return new SubscriptionCancelResponse(
                "구독 해지가 완료되었습니다. " + formattedDate + "까지는 프리미엄 혜택이 유지됩니다.",
                formattedDate,
                subscription.getStatus().name()
        );
    }
}