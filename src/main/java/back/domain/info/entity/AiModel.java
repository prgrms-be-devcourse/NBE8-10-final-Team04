package back.domain.info.entity;

import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiModel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private AiModelFamily family;       // ai_model_families.id 참조

    @Column(name = "model_name", nullable = false)
    private String modelName;           // 모델명 (예: Claude 3.5 Sonnet)

    @Column(name = "api_id", nullable = false, unique = true)
    private String apiId;               // 실제 호출 ID (예: claude-3-5-sonnet-20240620) → 고정 명칭

    @Column(name = "context_window")
    private Integer contextWindow;      // 입력 가능 토큰 한도

    @Column(name = "max_output_tokens")
    private Integer maxOutputTokens;    // 한 번에 출력할 수 있는 최대 토큰

    @Column(name = "release_date")
    private LocalDate releaseDate;      // 모델 공식 출시일

    @Column(name = "is_preview")
    private Boolean isPreview;          // 베타/프리뷰 모델 여부

    @Column(name = "model_image_url")
    private String modelImageUrl;       // 각 모델의 로고

    @Column(name = "input_price", precision = 10, scale = 6)
    private BigDecimal inputPrice;      // 제조사 공식 단가 (input)

    @Column(name = "output_price", precision = 10, scale = 6)
    private BigDecimal outputPrice;     // 제조사 공식 단가 (output)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_modalities", columnDefinition = "jsonb")
    private List<String> inputModalities = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_modalities", columnDefinition = "jsonb")
    private List<String> outputModalities = new ArrayList<>();

//    @Column(name = "category")
//    private String category;            // 특화 도메인 (= 카테고리? 태그?) → 추후 도입
}
