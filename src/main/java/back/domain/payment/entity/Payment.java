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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false, updatable=false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="subscription_id", nullable=false, updatable=false)
    private Subscription subscription;

    @Column(name = "order_id", updatable = false, nullable = false)
    private String orderId;

    @Column(name = "payment_key", updatable = false, nullable = false)
    private String paymentKey;

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

    @Builder
    public Payment(Member member, Subscription subscription, String orderId, String paymentKey ,Integer amount, PaymentStatus status, Map<String, Object> rawPayload) {
        this.member = member;
        this.subscription = subscription;
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = status;
        this.rawPayload = rawPayload;
        this.requestedAt = LocalDateTime.now();
    }

//    결제 성공 시 호출
    public void complete(LocalDateTime approvedAt, Map<String, Object> finalPayload) {
        this.status = PaymentStatus.PAID;
        this.approvedAt = approvedAt;
        this.paidAt = approvedAt;
        this.rawPayload = finalPayload;
    }


//    결제 실패 시 호출
    public void fail(String reason, Map<String, Object> errorPayload) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
        this.failedAt = LocalDateTime.now();
        this.rawPayload = errorPayload;
    }

}
