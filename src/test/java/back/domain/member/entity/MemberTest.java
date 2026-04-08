package back.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void createUser_setsUserRole() {
        Member member = Member.createUser("google-sub-1", "user1@example.com", "User One");

        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.isAdmin()).isFalse();
    }

    @Test
    void promoteToAdmin_changesRoleToAdmin() {
        Member member = Member.createUser("google-sub-2", "user2@example.com", "User Two");

        member.promoteToAdmin();

        assertThat(member.getRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(member.isAdmin()).isTrue();
    }

    @Test
    void updateName_changesName() {
        Member member = Member.createUser("google-sub-3", "user3@example.com", "Old Name");

        member.updateName("New Name");

        assertThat(member.getName()).isEqualTo("New Name");
    }
    @Test
    void createUser_initializesFreeUsageFields() {
        Member member = Member.createUser("google-sub-10", "user10@example.com", "User Ten");

        assertThat(member.getFreeUsageCount()).isEqualTo(0);
        assertThat(member.getFreeUsageResetAt()).isNotNull();
        assertThat(member.getLastUsageAt()).isNull();
        assertThat(member.getRemainingUsage()).isEqualTo(3);
        assertThat(member.canUseFree()).isTrue();
    }

    @Test
    void increaseFreeUsageCount_decreasesRemainingUsage() {
        Member member = Member.createUser("google-sub-11", "user11@example.com", "User Eleven");

        member.increaseFreeUsageCount();

        assertThat(member.getFreeUsageCount()).isEqualTo(1);
        assertThat(member.getRemainingUsage()).isEqualTo(2);
    }

    @Test
    void increaseFreeUsageCount_throwsExceptionWhenExceeded() {
        Member member = Member.createUser("google-sub-12", "user12@example.com", "User Twelve");

        member.increaseFreeUsageCount();
        member.increaseFreeUsageCount();
        member.increaseFreeUsageCount();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                member::increaseFreeUsageCount
        );

        assertThat(exception.getMessage()).isEqualTo("무료 사용 횟수를 초과하였습니다.");
    }

    @Test
    void updateLastUsageAt_setsLastUsageAt() {
        Member member = Member.createUser("google-sub-13", "user13@example.com", "User Thirteen");

        member.updateLastUsageAt();

        assertThat(member.getLastUsageAt()).isNotNull();
    }
}
