package back.domain.payment.controller;

import back.domain.payment.entity.SubscriptionStatus;
import back.domain.payment.service.UsageService;
import back.global.security.AuthenticatedMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import back.domain.payment.service.SubscriptionService;
import org.springframework.web.bind.annotation.RestController;

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
    public void useOnce(@AuthenticationPrincipal AuthenticatedMember member) {
        usageService.useOnce(member.memberId());
    }

    //    구독 상태 조회
    @GetMapping("")
    public SubscriptionStatus getSubscription(@AuthenticationPrincipal AuthenticatedMember member) {
        return subscriptionService.getStatus(member.memberId());
    }

    //    구독 해지
}
