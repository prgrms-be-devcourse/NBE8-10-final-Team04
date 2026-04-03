package back.domain.member.entity;

import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Column(name = "google_sub", nullable = false, length = 100, unique = true)
    private String googleSub;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    private Member(String googleSub, String email, String name, MemberRole role) {
        this.googleSub = googleSub;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public static Member createUser(String googleSub, String email, String name) {
        return new Member(
                requireNotBlank(googleSub, "googleSub"),
                requireNotBlank(email, "email"),
                requireNotBlank(name, "name"),
                MemberRole.USER);
    }

    public static Member createAdmin(String googleSub, String email, String name) {
        return new Member(
                requireNotBlank(googleSub, "googleSub"),
                requireNotBlank(email, "email"),
                requireNotBlank(name, "name"),
                MemberRole.ADMIN);
    }

    public void updateName(String name) {
        this.name = requireNotBlank(name, "name");
    }

    public void updateEmail(String email) {
        this.email = requireNotBlank(email, "email");
    }

    public void promoteToAdmin() {
        this.role = MemberRole.ADMIN;
    }

    public boolean isAdmin() {
        return this.role == MemberRole.ADMIN;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

//    구독 관련 컬럼 추가
    @Column(name = "free_usage_count")
    private int freeUsageCount;  //이번 달 사용 횟수

    @Column(name = "free_usage_reset_at")
    private LocalDateTime freeUsageResetAt;  //초기화 날짜 (다음달 1일)

    public void increaseFreeUsageCount() {
        this.freeUsageCount++;
    }

    public int getFreeUsageCount() {
        return this.freeUsageCount;
    }

}
