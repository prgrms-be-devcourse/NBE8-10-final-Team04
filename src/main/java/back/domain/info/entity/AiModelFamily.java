package back.domain.info.entity;

import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_model_families")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiModelFamily extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private AiVendor vendor;            // ai_vendors.id 참조

    @Column(name = "family_name", nullable = false)
    private String familyName;          // 패밀리 명 (예: GPT-5, Claude 4, Gemini 3)

    @Column(name = "common_description", columnDefinition = "TEXT")
    private String commonDescription;   // 시리즈 공통 특징 설명 (상세 페이지 상단 노출용)

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AiModel> models = new ArrayList<>();
}