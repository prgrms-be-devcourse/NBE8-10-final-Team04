package back.domain.payment.controller;

import back.domain.payment.dto.response.SubscriptionCancelResponse;
import back.domain.payment.dto.response.SubscriptionStatusResponse;
import back.domain.payment.entity.Subscription;
import back.domain.payment.service.UsageService;
import back.global.security.AuthenticatedMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import back.domain.payment.service.SubscriptionService;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions/me")
public class SubscriptionController {

    private final UsageService usageService;
    private final SubscriptionService subscriptionService;

    //    무료 사용 조회
    @GetMapping("/usage")
    public int getUsage(@AuthenticationPrincipal AuthenticatedMember member) {
        return usageService.getRemainingUsage(member.memberId());
    }

    //    무료 사용 차감
    @PostMapping("/usage")
    public Map<String, Object> useOnce(@AuthenticationPrincipal AuthenticatedMember member) {
        int remaining = usageService.useOnce(member.memberId());
        return Map.of(
                "message", "1회 사용 완료하였습니다.",
                "remaining", remaining
        );
    }

    //    구독 상태 조회
    @GetMapping("")
    public SubscriptionStatusResponse getSubscription(@AuthenticationPrincipal AuthenticatedMember member) {
        return subscriptionService.getStatus(member.memberId());
    }

    //    구독 해지
    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionCancelResponse> cancelSubscription(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        // 1. 서비스 호출 (환불 여부를 결과로 받음)
        boolean refunded = subscriptionService.cancelWithRefund(member.memberId());

        // 2. 결과에 따른 분기 처리
        if (refunded) {
            // [CASE 1] 환불 성공: 데이터가 삭제되었으므로 직접 응답 생성
            return ResponseEntity.ok(new SubscriptionCancelResponse(
                    "당일 결제 및 미사용 건으로 확인되어 전액 환불 및 해지 완료되었습니다.",
                    null,
                    "INACTIVE",
                    true
            ));
        } else {
            // [CASE 2] 일반 해지: 데이터가 남아있으므로 DB 조회 후 DTO 변환
            Subscription s = subscriptionService.findByMemberId(member.memberId());
            return ResponseEntity.ok(SubscriptionCancelResponse.from(s, false));
        }
    }

}
