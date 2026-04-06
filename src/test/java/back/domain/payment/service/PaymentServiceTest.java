package back.domain.payment.service;

import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.domain.payment.dto.request.PaymentPrepareRequest;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionPlanType;
import back.domain.payment.entity.SubscriptionStatus;
import back.domain.payment.repository.PaymentRepository;
import back.domain.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

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
}
