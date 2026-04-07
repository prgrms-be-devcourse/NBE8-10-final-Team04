package back.domain.prompt.search.dto.candidate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CandidateMetadataDto(
        Integer stars,
        Integer forks,

        @JsonProperty("updated_at")
        String updatedAt
) {
}
