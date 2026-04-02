package back.domain.payment.entity;

import back.domain.member.entity.Member;
import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private SubscriptionPlanType planType;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "next_billing_at")
    private LocalDateTime nextBillingAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "billing_key")
    private String billingKey;

    @Column(name = "pg_provider")
    private String pgProvider;

    @Builder
    public Subscription(Member member, SubscriptionPlanType planType, Integer amount, String billingKey, String pgProvider ,LocalDateTime nextBillingAt) {
        this.member = member;
        this.planType = planType;
        this.amount = amount;
        this.billingKey = billingKey;
        this.pgProvider = pgProvider;

        this.activate(nextBillingAt);
    }

    //    구독 활성화 및 재개
    public void activate(LocalDateTime nextBillingAt) {
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.nextBillingAt = nextBillingAt;
        this.canceledAt = null;
    }

    //    구독 취소
    public void cancel(){
        this.status = SubscriptionStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }


}
