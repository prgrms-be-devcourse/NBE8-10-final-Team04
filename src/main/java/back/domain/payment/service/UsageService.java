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

    public int getRemainingUsage(Long memberId) {
        Member member = getMember(memberId);
        return 3 - member.getFreeUsageCount();
    }

    @Transactional
    public void useOnce(Long memberId) {
        Member member = getMember(memberId);

        if (member.getFreeUsageCount() >= 3) {
            throw new RuntimeException("무료 사용 횟수 초과");
        }
        member.increaseFreeUsageCount();
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));
    }
}
