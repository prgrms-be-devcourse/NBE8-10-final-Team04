package back.domain.payment.dto.response;

import back.domain.payment.entity.SubscriptionStatus;

public record SubscriptionStatusResponse(
        SubscriptionStatus status,
        String message
) {}