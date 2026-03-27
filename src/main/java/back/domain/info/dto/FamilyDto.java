package back.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class FamilyDto {

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("common_description")
    private String commonDescription;

    private List<ModelDto> models;
}
