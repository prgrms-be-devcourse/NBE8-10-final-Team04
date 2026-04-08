package back.domain.payment.service;

import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.domain.payment.dto.request.PaymentConfirmRequest;
import back.domain.payment.dto.request.PaymentPrepareRequest;
import back.domain.payment.dto.response.PaymentPrepareResponse;
import back.domain.payment.entity.Payment;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionPlanType;
import back.domain.payment.repository.PaymentRepository;
import back.domain.payment.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${toss.secret-key}") // yml에 있는 값을 자동으로 가져옵니다.
    private String secretKey;

    @Transactional
    public PaymentPrepareResponse prepare(Long memberId, PaymentPrepareRequest request){

        // planType 검증
        if(request.planType() == null){
            throw new IllegalArgumentException("결제할 구독 상품을 선택해주세요.");
        }

        //  회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 중복결제 방지 로직
        subscriptionRepository.findByMemberId(memberId)
                .ifPresent(subscription -> {
                    if(subscription.isActive()){
                        throw new IllegalArgumentException("이미 이용 중인 구독권이 있습니다. 만료 후 다시 시도해주세요.");
                    }
                });

        //  주문번호 생성
        String orderId = "SUB-" + UUID.randomUUID();

        //  결제 엔티티 생성
        Payment payment = Payment.createReady(member, orderId, request.planType());

        //  DB에 저장
        paymentRepository.save(payment);

        return PaymentPrepareResponse.from(payment);

    }
    
    //    결제 승인하기
    @Transactional
    public Payment confirm(Long memberId, PaymentConfirmRequest request) {
        // 1. DB에서 결제 대기 데이터 조회
        Payment payment = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));


        // 2. ID 검증
        if (!payment.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 결제만 승인할 수 있습니다.");
        }

        // 2-1. 결제상태 확인
        if (!payment.isReady()) {
            throw new IllegalStateException("승인 가능한 결제 상태가 아닙니다.");
        }

        // 2-2. 금액 검증 (중요: 프론트에서 보낸 금액과 DB 금액이 같은지 확인)
        if (payment.getAmount().longValue() != request.amount().longValue()) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }


        // 3. 토스페이먼츠 승인 API 호출
        // [테스트 단계] 실제 토스 서버와 통신하는 private 메서드를 호출합니다.
//        confirmToToss(request);

        // 4. 결제 상태 변경 (READY -> DONE)
        payment.markAsDone();
        payment.updatePaymentKey(request.paymentKey());


        // 5. 구독 정보 생성/갱신 로직 (여기에 추가)
        Subscription subscription = updateSubscription(payment.getMember(), payment.getPlanType());
        payment.assignSubscription(subscription);

        return payment;
    }

    private Subscription updateSubscription(Member member, SubscriptionPlanType planType) {
        // 기존 구독이 있으면 업데이트, 없으면 생성
        Subscription subscription = subscriptionRepository.findByMemberId(member.getId())
                .orElseGet(() -> Subscription.builder()
                        .member(member)
                        .build());

        // 구독 활성화 및 종료일 설정 (예: 1개월)
        subscription.activate(planType, java.time.LocalDateTime.now().plusMonths(1));
        return subscriptionRepository.save(subscription);
    }

        /**
         * 토스페이먼츠 API 서버로 최종 승인 요청을 보냅니다.
         */
        private void confirmToToss(PaymentConfirmRequest request) {
            String basicAuth = "Basic " + java.util.Base64.getEncoder()
                    .encodeToString((secretKey + ":").getBytes(java.nio.charset.StandardCharsets.UTF_8));

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", basicAuth);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<PaymentConfirmRequest> entity = new org.springframework.http.HttpEntity<>(request, headers);

            try {
                org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                        "https://api.tosspayments.com/v1/payments/confirm",
                        entity,
                        String.class
                );

                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("토스 결제 승인 실패: " + response.getBody());
                }
            } catch (Exception e) {
                throw new RuntimeException("결제 통신 중 오류 발생: " + e.getMessage());
            }
    }

}
