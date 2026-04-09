package back.domain.payment.dto.response;

import back.domain.payment.entity.Payment;
import back.domain.payment.entity.SubscriptionPlanType;
import lombok.Builder;

@Builder
public record PaymentPrepareResponse(
        String orderId,
        String orderName,
        int amount,
        SubscriptionPlanType planType
) {
    public static PaymentPrepareResponse from(Payment payment) {
        return new PaymentPrepareResponse(
                payment.getOrderId(),
                payment.getPlanType().getTitle(),
                payment.getAmount(),
                payment.getPlanType()
        );
    }
}
