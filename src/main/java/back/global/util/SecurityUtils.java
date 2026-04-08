package back.global.util;

import back.global.security.AuthenticatedMember;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    private SecurityUtils() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }

        if (authentication.getPrincipal() instanceof AuthenticatedMember) {
            return ((AuthenticatedMember) authentication.getPrincipal()).memberId();
        }

        throw new RuntimeException("인증 객체 타입이 일치하지 않습니다.");
    }
}
