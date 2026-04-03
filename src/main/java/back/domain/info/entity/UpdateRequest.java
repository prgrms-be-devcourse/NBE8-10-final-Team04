package back.domain.info.entity;

import back.domain.info.enums.Status;
import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "update_requests")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UpdateRequest extends BaseEntity {

    private String sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private AiVendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = true)
    private AiModelFamily family;

    private String sourceUrl;

    private String sourceType;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;  // PENDING, APPROVED, REJECTED

    private LocalDate notifiedAt;// 업데이트 일시

    private LocalDateTime reviewedAt;   // 승인 / 반영 일시
}

