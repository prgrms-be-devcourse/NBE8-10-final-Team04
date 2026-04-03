package back.domain.aitracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * OCI data/ai-info/integrated_major_models.json 파싱용 DTO.
 *
 * <p>family_name 매칭에만 사용하며, description 등 불필요한 필드는 포함하지 않습니다.
 */
public record IntegratedVendorRef(
        String name,
        @JsonProperty("is_active") boolean isActive,
        List<FamilyRef> families) {

    /** family_name 참조용 최소 구조. */
    public record FamilyRef(
            @JsonProperty("family_name") String familyName) {}
}
