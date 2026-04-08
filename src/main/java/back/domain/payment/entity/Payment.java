package back.domain.payment.entity;

import back.domain.member.entity.Member;
import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "payment")
@Getter
@SuppressWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false, updatable=false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="subscription_id", nullable = true)
    private Subscription subscription;

    @Column(name = "order_id", updatable = false, nullable = true)
    private String orderId;

    @Column(name = "payment_key", nullable = true)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private SubscriptionPlanType planType;

    @Column(name = "amount")
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "fail_reason")
    private String failReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private Map<String, Object> rawPayload;

    @SuppressWarnings("EI_EXPOSE_REP")
    public Member getMember() {
        return member;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public Subscription getSubscription() {
        return subscription;
    }

    @SuppressWarnings("EI_EXPOSE_REP")
    public Map<String, Object> getRawPayload() {
        return rawPayload;
    }

    @Builder(access = AccessLevel.PRIVATE)
    @SuppressWarnings("EI_EXPOSE_REP2")
    public Payment(Member member, Subscription subscription, String orderId, String paymentKey,
                   PaymentStatus status, SubscriptionPlanType planType, Map<String, Object> rawPayload) {
        this.member = member;
        this.subscription = subscription;
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = planType.getAmount();
        this.status = status;
        this.planType = planType;
        this.rawPayload = rawPayload;
        this.requestedAt = LocalDateTime.now();
    }

//    결제 준비
    public static Payment createReady(Member member, String orderId, SubscriptionPlanType planType){
        return Payment.builder()
                .member(member)
                .orderId(orderId)
                .planType(planType)
                .status(PaymentStatus.READY)
                .build();
    }

//    결제 성공 시 호출
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void complete(LocalDateTime approvedAt, Map<String, Object> finalPayload) {
        this.status = PaymentStatus.PAID;
        this.approvedAt = approvedAt;
        this.paidAt = approvedAt;
        this.rawPayload = finalPayload;
    }


//    결제 실패 시 호출
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void fail(String reason, Map<String, Object> errorPayload) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
        this.failedAt = LocalDateTime.now();
        this.rawPayload = errorPayload;
    }

    public void markAsDone() {
        // 결제 상태를 완료(PAID)로 변경
        // 만약 필드명이 status가 아니라면 본인의 엔티티 필드명에 맞게 수정하세요.
        this.status = PaymentStatus.PAID;
    }

//    상태확인
    public boolean isReady() {
        return this.status == PaymentStatus.READY;
    }

    /**
     * 결제 완료 시 해당 결제가 어떤 구독에 속하는지 연결합니다.
     */
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void assignSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    /**
     * 결제 승인 완료 후 토스에서 발급한 paymentKey를 저장합니다.
     */
    public void updatePaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 결제 키입니다.");
        }
        this.paymentKey = paymentKey;
    }

//    결제 취소
    public void markAsCanceled() {
        // 이미 취소된 상태라면 에러를 던져서 중복 환불을 방지합니다.
        if (this.status == PaymentStatus.CANCELED) {
            throw new IllegalStateException("이미 취소된 결제 건입니다.");
        }

        // 상태를 CANCELED로 변경! (슬기님이 만든 Enum 이름에 맞춰주세요)
        this.status = PaymentStatus.CANCELED;
    }

    public void unlinkSubscription() {
        this.subscription = null; // 구독 참조를 제거
    }

}
