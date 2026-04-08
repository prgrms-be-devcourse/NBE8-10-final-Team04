package back.domain.payment.service;

import back.domain.member.entity.Member;
import back.domain.payment.dto.response.SubscriptionStatusResponse;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionPlanType; // 추가된 임포트
import back.domain.payment.entity.SubscriptionStatus;
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

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

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
}