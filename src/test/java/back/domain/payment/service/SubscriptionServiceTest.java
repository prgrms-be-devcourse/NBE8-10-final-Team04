package back.domain.payment.service;

import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionPlanType; // 추가된 임포트
import back.domain.payment.entity.SubscriptionStatus;
import back.domain.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        // given
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
        SubscriptionStatus result = subscriptionService.getStatus(memberId);

        // then
        assertThat(result).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("구독 정보가 없으면 INACTIVE를 반환한다")
    void getStatus_whenSubscriptionDoesNotExist() {
        // given
        Long memberId = 1L;

        given(subscriptionRepository.findByMemberId(memberId))
                .willReturn(Optional.empty());

        // when
        SubscriptionStatus result = subscriptionService.getStatus(memberId);

        // then
        assertThat(result).isEqualTo(SubscriptionStatus.INACTIVE);
    }
}