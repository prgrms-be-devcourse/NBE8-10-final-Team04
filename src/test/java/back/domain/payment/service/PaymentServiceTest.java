package back.domain.payment.service;

import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.domain.payment.dto.request.PaymentConfirmRequest;
import back.domain.payment.dto.request.PaymentPrepareRequest;
import back.domain.payment.entity.*;
import back.domain.payment.repository.PaymentRepository;
import back.domain.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private Member member;
    private Payment payment;

    @BeforeEach
    void setUp() {
        member = Mockito.mock(Member.class);
        given(member.getId()).willReturn(1L);

        // 결제 대기(READY) 상태의 객체 생성
        payment = Payment.createReady(member, "order-123", SubscriptionPlanType.MONTHLY_990);
    }


    @Test
    void 이미_구독중이면_에러가_발생한다() {
        // Member 생성 (기존 메서드 활용)
        Member member = Member.createUser("sub", "email", "name");
        ReflectionTestUtils.setField(member, "id", 1L); // private id 필드에 강제 주입

        // Subscription 생성 (만약 빌더/Setter가 없다면)
        // 1. 기본 생성자로 일단 만듦 (접근 가능 시)
        Subscription activeSub = org.mockito.Mockito.mock(Subscription.class);

        // 2. private 필드인 status에 직접 값 주입
        given(activeSub.isActive()).willReturn(true);
    //  ReflectionTestUtils.setField(activeSub, "status", SubscriptionStatus.ACTIVE);

        // Mock 설정 (괄호 위치 주의!)
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(subscriptionRepository.findByMemberId(1L)).willReturn(Optional.of(activeSub));

        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.prepare(1L, new PaymentPrepareRequest(SubscriptionPlanType.MONTHLY_990));
        });
    }

    @Test
    @DisplayName("결제 승인 성공 - 상태가 PAID(DONE)로 바뀌고 구독이 생성되어야 한다")
    void confirm_Success() {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("key-123", "order-123", 990L);

        given(paymentRepository.findByOrderId("order-123")).willReturn(Optional.of(payment));
        given(subscriptionRepository.findByMemberId(1L)).willReturn(Optional.empty());

        // save() 호출 시 인자로 넘어온 객체를 그대로 반환하도록 설정
        given(subscriptionRepository.save(any(Subscription.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Payment result = paymentService.confirm(1L, request);

        // then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID); // 아까 PAID로 확인했죠!
        assertThat(result.getPaymentKey()).isEqualTo("key-123");
        assertThat(result.getSubscription()).isNotNull();
    }

    @Test
    @DisplayName("결제 승인 실패 - 금액이 일치하지 않는 경우")
    void confirm_Fail_AmountMismatch() {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("key-123", "order-123", 50000L); // 잘못된 금액
        given(paymentRepository.findByOrderId("order-123")).willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentService.confirm(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("결제 금액이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("결제 승인 실패 - 다른 사용자의 주문인 경우")
    void confirm_Fail_MemberMismatch() {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("key-123", "order-123", 990L);
        given(paymentRepository.findByOrderId("order-123")).willReturn(Optional.of(payment));

        // when & then
        // 다른 회원 ID(2L)로 요청 시도
        assertThatThrownBy(() -> paymentService.confirm(2L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인의 결제만 승인할 수 있습니다.");
    }

}
