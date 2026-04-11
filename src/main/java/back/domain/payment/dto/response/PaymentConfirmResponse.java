package back.domain.payment.dto.response;

import back.domain.payment.entity.Payment;

public record PaymentConfirmResponse(
        String orderId,
        Long amount,
        String status,
        String message
) {
    public static PaymentConfirmResponse success(Payment payment) {
        return new PaymentConfirmResponse(
                payment.getOrderId(),
                payment.getAmount().longValue(),
                payment.getStatus().name(),
                "결제가 최종 승인되었습니다."
        );
    }
}