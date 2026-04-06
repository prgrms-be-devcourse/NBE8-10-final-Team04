package back.domain.payment.entity;

import lombok.Getter;

@Getter
public enum SubscriptionPlanType {
//    추후 플랜 추가를 위한 enum 설계
    MONTHLY_990("월간 구독", 990); // 기본 요금

    private final String title;
    private final int amount;

    SubscriptionPlanType(String title, int amount) {
        this.title = title;
        this.amount = amount;
    }
}
