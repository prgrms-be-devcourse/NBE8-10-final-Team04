package back.domain.payment.entity;

public enum SubscriptionStatus {
    ACTIVE, //정상 구독중
    INACTIVE, // 구독 안함
    CANCELED, //해지
    PAST_DUE  // 결제 실패
}
