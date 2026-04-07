package back.domain.payment.controller;

import back.domain.member.entity.Member;
import back.domain.payment.dto.request.PaymentConfirmRequest;
import back.domain.payment.dto.request.PaymentPrepareRequest;
import back.domain.payment.dto.response.PaymentPrepareResponse;
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
//            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestBody PaymentPrepareRequest request
    ) {
        Long tempMemberId = 225L; // 테스트 후 실제 연동 시 삭제
        return paymentService.prepare(tempMemberId, request);
//        return paymentService.prepare(authenticatedMember.memberId(), request);
    }

    //    결제 승인
    @PostMapping("/confirm")
    public ResponseEntity<String> confirm(
//            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestBody PaymentConfirmRequest request
    ) {
        Long tempMemberId = 225L;// 테스트 후 실제 연동 시 삭제
        paymentService.confirm(tempMemberId, request);

//        paymentService.confirm(authenticatedMember.memberId(), request);
        return ResponseEntity.ok("결제가 최종 승인되었습니다.");
    }

//    =====================================================================

    //    결제 이력 조회
    // @GetMapping("/me")
    
    //    자동결제 웹훅 수신

}
