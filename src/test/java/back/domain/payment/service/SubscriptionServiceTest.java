package back.domain.payment.service;

import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.domain.payment.dto.response.SubscriptionStatusResponse;
import back.domain.payment.entity.Payment;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionPlanType;
import back.domain.payment.entity.SubscriptionStatus;
import back.domain.payment.repository.PaymentRepository;
import back.domain.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("구독 정보가 있으면 해당 상태를 반환한다")
    void getStatus_whenSubscriptionExists() {
        Long memberId = 1L;

        Subscription subscription = Subscription.builder()
                .member(Mockito.mock(Member.class))
                .planType(SubscriptionPlanType.MONTHLY_990)
                .amount(9900)
                .nextBillingAt(LocalDateTime.now().plusMonths(1))
                .build();

        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.of(subscription));

        SubscriptionStatusResponse result = subscriptionService.getStatus(memberId);

        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.message()).isEqualTo("구독 중");
    }

    @Test
    @DisplayName("구독 정보가 없으면 INACTIVE를 반환한다")
    void getStatus_whenSubscriptionDoesNotExist() {
        Long memberId = 1L;

        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.empty());

        SubscriptionStatusResponse result = subscriptionService.getStatus(memberId);

        assertThat(result.status()).isEqualTo(SubscriptionStatus.INACTIVE);
        assertThat(result.message()).isEqualTo("구독 중이 아닙니다.");
    }

    @Test
    @DisplayName("구독 해지 성공 - 상태가 CANCELED로 변경되어야 한다")
    void cancelSubscription_Success() {
        Long memberId = 1L;
        Member member = Mockito.mock(Member.class);

        Subscription subscription = Subscription.builder()
                .member(member)
                .planType(SubscriptionPlanType.MONTHLY_990)
                .amount(9900)
                .nextBillingAt(LocalDateTime.now().plusMonths(1))
                .build();

        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.of(subscription));

        Subscription result = subscriptionService.cancelSubscription(memberId);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(result.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("구독 해지 실패 - 활성화된 구독이 없는 경우")
    void cancelSubscription_Fail_NotActive() {
        Long memberId = 1L;

        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해지할 수 있는 활성 구독권이 없습니다.");
    }

    @Test
    @DisplayName("환불 성공 - 당일 결제했고 사용 기록이 없으면 구독을 삭제한다")
    void cancelWithRefund_Success() {
        Long memberId = 1L;
        Payment mockPayment = Mockito.mock(Payment.class);
        Member member = Member.createUser("google_123", "test@test.com", "슬기");

        given(paymentRepository.findTodayPaymentByMemberId(memberId))
                .willReturn(Optional.of(mockPayment));
        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));

        boolean refunded = subscriptionService.cancelWithRefund(memberId);

        assertThat(refunded).isTrue();
        verify(mockPayment).markAsCanceled();
        verify(mockPayment).unlinkSubscription();
        verify(paymentRepository).save(mockPayment);
        verify(subscriptionRepository).deleteByMemberId(memberId);
    }

    @Test
    @DisplayName("환불 실패 - 결제 이후 AI 사용 기록이 있으면 일반 해지 처리")
    void cancelWithRefund_Fail_UsedAfterPayment() {
        Long memberId = 1L;
        Payment mockPayment = Mockito.mock(Payment.class);
        Subscription mockSubscription = Mockito.mock(Subscription.class);
        Member member = Member.createUser("google_123", "test@test.com", "슬기");

        LocalDateTime paymentTime = LocalDateTime.now().minusHours(2);

        given(paymentRepository.findTodayPaymentByMemberId(memberId))
                .willReturn(Optional.of(mockPayment));
        given(mockPayment.getCreatedAt())
                .willReturn(paymentTime);
        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));
        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.of(mockSubscription));
        given(mockSubscription.isActive())
                .willReturn(true);

        member.updateLastUsageAt(); // 현재 시간이 paymentTime보다 뒤 -> 환불 실패, 일반 해지

        boolean refunded = subscriptionService.cancelWithRefund(memberId);

        assertThat(refunded).isFalse();
        verify(mockSubscription).cancel();
        verify(subscriptionRepository, never()).deleteByMemberId(memberId);
        verify(paymentRepository, never()).save(mockPayment);
    }

    @Test
    @DisplayName("구독 조회 실패 - 구독 정보가 없으면 예외가 발생한다")
    void findByMemberId_Fail() {
        Long memberId = 1L;

        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.findByMemberId(memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("구독 정보를 찾을 수 없습니다.");
    }
}