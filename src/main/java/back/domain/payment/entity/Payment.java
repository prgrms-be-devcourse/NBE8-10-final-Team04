package back.domain.payment.entity;

import back.domain.member.entity.Member;
import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="subscription_id")
    private Subscription subscription;

    @Column(name = "order_id", updatable = false)
    private String orderId;

    @Column(name = "payment_key")
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

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Member getMember() {
        return member;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Subscription getSubscription() {
        return subscription;
    }

    public Map<String, Object> getRawPayload() {
        return rawPayload == null ? null : new HashMap<>(rawPayload);
    }

    @Builder(access = AccessLevel.PRIVATE)
    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    private Payment(Member member, Subscription subscription, String orderId, String paymentKey,
                    PaymentStatus status, SubscriptionPlanType planType, Map<String, Object> rawPayload) {
        this.member = member;
        this.subscription = subscription;
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = (planType != null) ? planType.getAmount() : 0;
        this.status = status;
        this.planType = planType;
        this.rawPayload = rawPayload == null ? null : new HashMap<>(rawPayload);
        this.requestedAt = LocalDateTime.now();
    }

    public static Payment createReady(Member member, String orderId, SubscriptionPlanType planType){
        return Payment.builder()
                .member(member)
                .orderId(orderId)
                .planType(planType)
                .status(PaymentStatus.READY)
                .build();
    }

    public void complete(LocalDateTime approvedAt, Map<String, Object> finalPayload) {
        this.status = PaymentStatus.PAID;
        this.approvedAt = approvedAt;
        this.paidAt = approvedAt;
        this.rawPayload = finalPayload == null ? null : new HashMap<>(finalPayload);
    }

    public void fail(String reason, Map<String, Object> errorPayload) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
        this.failedAt = LocalDateTime.now();
        this.rawPayload = errorPayload == null ? null : new HashMap<>(errorPayload);
    }

    public void markAsDone() {
        this.status = PaymentStatus.PAID;
    }

    public boolean isReady() {
        return this.status == PaymentStatus.READY;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void assignSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public void updatePaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 결제 키입니다.");
        }
        this.paymentKey = paymentKey;
    }

    public void markAsCanceled() {
        if (this.status == PaymentStatus.CANCELED) {
            throw new IllegalStateException("이미 취소된 결제 건입니다.");
        }
        this.status = PaymentStatus.CANCELED;
    }

    public void unlinkSubscription() {
        this.subscription = null;
    }
}