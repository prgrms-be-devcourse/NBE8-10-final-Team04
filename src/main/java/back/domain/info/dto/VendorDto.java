package back.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class VendorDto {
    private String name;

    @JsonProperty("official_url")
    private String officialUrl;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("is_deprecated")
    private Boolean isDeprecated;

    private List<FamilyDto> families;
}
