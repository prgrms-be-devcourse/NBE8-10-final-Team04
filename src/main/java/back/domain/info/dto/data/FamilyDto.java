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
    public FamilyDto {
        inputTypes = inputTypes == null ? null : inputTypes.clone();
        outputTypes = outputTypes == null ? null : outputTypes.clone();
    }

    @Override
    public String[] inputTypes() {
        return inputTypes == null ? null : inputTypes.clone();
    }

    @Override
    public String[] outputTypes() {
        return outputTypes == null ? null : outputTypes.clone();
    }
}
