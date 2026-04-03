package back.domain.payment.controller;

import back.domain.payment.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions/me")
public class SubscriptionController {

    private final UsageService usageService;

    @GetMapping("/usage")
    public int getUsage() {
        Long memberId = 1L;
        return usageService.getRemainingUsage(memberId);
    }

    @PostMapping("/usage")
    public void useOnce() {
        Long memberId = 1L;
        usageService.useOnce(memberId);
    }

}
