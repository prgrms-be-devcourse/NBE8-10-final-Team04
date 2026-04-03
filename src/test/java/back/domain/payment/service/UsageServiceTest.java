package back.domain.payment.service;

import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsageServiceTest {

    private UsageService usageService;
    private MemberRepository memberRepository;
    private Member member;

    @BeforeEach
    void setUp() {
        memberRepository = Mockito.mock(MemberRepository.class);
        usageService = new UsageService(memberRepository);

        member = Member.createUser("google-sub-123", "test@test.com", "슬기");

        // 기본 설정: id 1 요청하면 member 반환
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    }

    @Test
    @DisplayName("무료 사용 남은 횟수를 반환한다")
    void getRemainingUsage() {
        int remaining = usageService.getRemainingUsage(1L);

        assertEquals(3, remaining);
    }

    @Test
    @DisplayName("무료 사용 1회를 차감하면 freeUsageCount가 1 증가한다")
    void useOnce() {
        usageService.useOnce(1L);

        assertEquals(1, member.getFreeUsageCount());
    }

    @Test
    @DisplayName("무료 사용 3회를 초과하면 예외가 발생한다")
    void useOnce_fail_whenExceeded() {
        usageService.useOnce(1L);
        usageService.useOnce(1L);
        usageService.useOnce(1L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            usageService.useOnce(1L);
        });

        assertEquals("무료 사용 횟수 초과", exception.getMessage());
    }
}