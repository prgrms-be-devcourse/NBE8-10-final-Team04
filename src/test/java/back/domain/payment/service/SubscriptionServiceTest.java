package back.domain.payment.service;

import back.domain.member.entity.Member;
import back.domain.payment.dto.response.SubscriptionStatusResponse;
import back.domain.payment.entity.Payment;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionPlanType; // 추가된 임포트
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UsageService usageService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private Subscription subscription;


    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("구독 정보가 있으면 해당 상태를 반환한다")
    void getStatus_whenSubscriptionExists() {
        Long memberId = 1L;

        Subscription subscription = Subscription.builder()
                .member(null)
                .planType(SubscriptionPlanType.MONTHLY_990)
                .amount(2000)
                .nextBillingAt(LocalDateTime.now().plusMonths(1))
                .build();

        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.of(subscription));

        // when
        SubscriptionStatusResponse result = subscriptionService.getStatus(memberId);

        // then
        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.message()).isEqualTo("구독 중");
    }

    @Test
    @DisplayName("구독 정보가 없으면 INACTIVE를 반환한다")
    void getStatus_whenSubscriptionDoesNotExist() {
        Long memberId = 1L;

        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.empty());

        // when
        SubscriptionStatusResponse result = subscriptionService.getStatus(memberId);

        // then
        assertThat(result.status()).isEqualTo(SubscriptionStatus.INACTIVE);
        assertThat(result.message()).isEqualTo("구독 중이 아닙니다.");
    }

    @Test
    @DisplayName("구독 해지 성공 - 상태가 CANCELED로 변경되어야 한다")
    void cancelSubscription_Success() {
        // given
        Long memberId = 1L;
        Member member = Mockito.mock(Member.class);
        Subscription subscription = Subscription.builder()
                .member(member)
                .planType(SubscriptionPlanType.MONTHLY_990)
                .nextBillingAt(LocalDateTime.now().plusMonths(1))
                .build(); // 빌더 내부에서 기본적으로 ACTIVE로 설정됨

        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.of(subscription));

        // when
        subscriptionService.cancelSubscription(memberId);

        // then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(subscription.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("구독 해지 실패 - 활성화된 구독이 없는 경우")
    void cancelSubscription_Fail_NotActive() {
        // given
        Long memberId = 1L;
        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해지할 수 있는 활성 구독권이 없습니다.");
    }

    @Test
    @DisplayName("환불 성공 - 당일 결제했고 사용 기록이 없으면 구독을 삭제한다")
    void cancelWithRefund_Success() {
        back.domain.payment.entity.Payment mockPayment = Mockito.mock(back.domain.payment.entity.Payment.class);
        // given
        given(paymentRepository.findTodayPaymentByMemberId(1L)).willReturn(Optional.of(mockPayment));
        given(usageService.getUsageCountForToday(1L)).willReturn(0); // 사용량 0

        // when
        subscriptionService.cancelWithRefund(1L);

        // then
        verify(subscriptionRepository).deleteByMemberId(1L); // 삭제 메서드 호출 확인
        verify(mockPayment).markAsCanceled(); // 결제 취소 마킹 확인
    }

    @Test
    @DisplayName("환불 실패 후 일반 해지 - 당일 결제했지만 사용 기록이 있으면 일반 해지 처리한다")
    void cancelWithRefund_Fail_AlreadyUsed() {
        back.domain.payment.entity.Payment mockPayment = Mockito.mock(back.domain.payment.entity.Payment.class);
        // given
        given(paymentRepository.findTodayPaymentByMemberId(1L)).willReturn(Optional.of(mockPayment));
        given(usageService.getUsageCountForToday(1L)).willReturn(1); // 이미 1번 사용함

        // when
        subscriptionService.cancelWithRefund(1L);

        // then
        verify(subscription).cancel(); // delete 대신 cancel(해지예약)이 호출되어야 함
    }

    @Test
    @DisplayName("환불 실패 - 결제 이후 AI 사용 기록이 있으면 일반 해지 처리")
    void cancelWithRefund_Fail_UsedAfterPayment() {
        // given
        Long memberId = 1L;
        Payment mockPayment = Mockito.mock(Payment.class);
        Subscription mockSubscription = Mockito.mock(Subscription.class);

        // 1. [수정] builder() 대신 createUser() 사용
        // Member 엔티티에 있는 정적 팩토리 메서드를 활용합니다.
        Member member = Member.createUser("google_123", "test@test.com", "슬기");

        // 2. 결제 시간 설정 (오후 1시)
        given(paymentRepository.findTodayPaymentByMemberId(memberId)).willReturn(Optional.of(mockPayment));
        given(mockPayment.getCreatedAt()).willReturn(LocalDateTime.now().withHour(13).withMinute(0));

        // 3. 마지막 사용 시간 설정 (오후 2시 -> 결제 이후 사용함)
        member.updateLastUsageAt(); // 현재 시간으로 갱신되거나, 필요시 reflection으로 시간을 주입

        // 4. [추가] 서비스 로직에서 구독권을 찾을 수 있도록 설정
        given(subscriptionRepository.findByMemberId(memberId)).willReturn(Optional.of(mockSubscription));

        // when
        subscriptionService.cancelWithRefund(memberId);

        // then
        // 사용 기록이 있으므로 delete가 아닌 cancel()이 호출되어야 함
        verify(mockSubscription).cancel();
    }
}