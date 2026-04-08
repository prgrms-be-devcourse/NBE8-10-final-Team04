package back.domain.payment.controller;

import back.domain.member.entity.Member;
import back.domain.payment.dto.request.PaymentConfirmRequest;
import back.domain.payment.dto.request.PaymentPrepareRequest;
import back.domain.payment.dto.response.PaymentConfirmResponse;
import back.domain.payment.dto.response.PaymentPrepareResponse;
import back.domain.payment.entity.Payment;
import back.domain.payment.service.PaymentService;
import back.global.security.AuthenticatedMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    //    구독 결제 준비/준비서 생성
    @PostMapping("/prepare")
    public PaymentPrepareResponse prepare(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember, // 주석 해제!
            @RequestBody PaymentPrepareRequest request
    ) {
        return paymentService.prepare(authenticatedMember.memberId(), request);
    }

    // 결제 승인
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestBody PaymentConfirmRequest request
    ) {
        // 이제 서비스의 confirm 메서드가 Payment 객체를 반환하도록 살짝 고쳐볼까요?
        Payment payment = paymentService.confirm(authenticatedMember.memberId(), request);
        return ResponseEntity.ok(PaymentConfirmResponse.success(payment));
    }

//    =====================================================================

    //    결제 이력 조회
    // @GetMapping("/me")
    
    //    자동결제 웹훅 수신

}
