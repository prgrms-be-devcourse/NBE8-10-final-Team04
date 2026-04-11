package back.domain.payment.dto.request;

import back.domain.payment.entity.SubscriptionPlanType;
import jakarta.validation.constraints.NotNull;

public record PaymentPrepareRequest(
        @NotNull(message = "구독 플랜 선택은 필수입니다.")
        @com.fasterxml.jackson.annotation.JsonProperty("planType")
        SubscriptionPlanType planType
) {
}
