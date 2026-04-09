package back.global.config;

import back.domain.auth.service.McpTokenAuthenticationService;
import back.domain.payment.entity.Subscription;
import back.domain.payment.repository.SubscriptionRepository;
import back.global.security.AuthenticatedMember;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class SubscriptionAspect {
    private final SubscriptionRepository subscriptionRepository;
    private final McpTokenAuthenticationService mcpTokenAuthenticationService;

    @Before("@annotation(back.global.annotation.RequiresSubscription)")
    public void checkSubscription() {
        Long memberId = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof AuthenticatedMember authenticatedMember) {
            memberId = authenticatedMember.memberId();
        } else {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String authHeader = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                memberId = mcpTokenAuthenticationService.authenticate(authHeader);
            }
        }

        if (memberId == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        Subscription subscription = subscriptionRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("구독이 필요한 서비스입니다."));

        if (!subscription.isActive()) {
            throw new IllegalStateException("구독 기간이 만료되었습니다. 결제 후 이용해주세요.");
        }
    }
}