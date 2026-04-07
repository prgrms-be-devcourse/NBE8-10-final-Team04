package back.domain.info.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FamilyDto(
        @JsonProperty("family_name")
        String familyName,

        @JsonProperty("common_description")
        String commonDescription,

        @JsonProperty("input_types")
        String[] inputTypes,

        @JsonProperty("output_types")
        String[] outputTypes
) {
}
