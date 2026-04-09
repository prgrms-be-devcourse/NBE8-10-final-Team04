package back.domain.payment.dto.response;

import back.domain.payment.entity.Subscription;

public record SubscriptionCancelResponse(
        String message,
        String nextBillingAt,
        String status,
        boolean isRefunded
) {
    public static SubscriptionCancelResponse from(Subscription subscription, boolean isRefunded) {
        String formattedDate = subscription.getNextBillingAt()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));

        String message = isRefunded
                ? "전액 환불 및 해지가 완료되었습니다."
                : "이미 사용 기록이 있어 혜택 유지 후 " + formattedDate + "에 해지됩니다.";

        return new SubscriptionCancelResponse(
                message,
                formattedDate,
                subscription.getStatus().name(),
                isRefunded // 여기서 결과값을 담아줍니다!
        );
    }
}