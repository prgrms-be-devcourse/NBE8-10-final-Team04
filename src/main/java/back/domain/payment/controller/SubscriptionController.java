package back.domain.payment.controller;

import back.domain.payment.dto.response.SubscriptionCancelResponse;
import back.domain.payment.dto.response.SubscriptionStatusResponse;
import back.domain.payment.entity.Subscription;
import back.domain.payment.entity.SubscriptionStatus;
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

import java.time.format.DateTimeFormatter;
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
        // 서비스는 엔티티를 반환하지만, 컨트롤러에서 바로 DTO로 변환합니다.
        Subscription subscription = subscriptionService.cancelSubscription(member.memberId());

        return ResponseEntity.ok(SubscriptionCancelResponse.from(subscription));
    }

}
