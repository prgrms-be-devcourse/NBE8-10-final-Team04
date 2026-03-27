package back.domain.info.entity;

import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiVendor extends BaseEntity {

    @Column(nullable = false)
    private String name;                // OpenAI, Anthropic, Google 등

    @Column(name = "official_url")
    private String officialUrl;         // 공식 사이트 주소

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;           // 현재 서비스 중 여부

    @Column(name = "is_deprecated", nullable = false)
    private Boolean isDeprecated;       // 지원 종료 여부

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AiModelFamily> modelFamilies = new ArrayList<>();
}
