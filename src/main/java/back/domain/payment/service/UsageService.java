package back.domain.payment.service;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsageService {

    private final MemberRepository memberRepository;
    
    @Transactional
    public int getRemainingUsage(Long memberId) {
        Member member = getMember(memberId);
        member.resetFreeUsageIfNeeded();
        return member.getRemainingUsage();
    }

    @Transactional
    public void useOnce(Long memberId) {
        Member member = getMember(memberId);
        member.resetFreeUsageIfNeeded();
        member.increaseFreeUsageCount();
    }
    
    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));
    }
}
