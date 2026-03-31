package back.domain.prompt.search.dto.candidate;

import back.domain.prompt.prompt.enums.Category;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CandidateDto {

    @JsonProperty("skill_id")
    private Long skillId;

    @JsonProperty("skill_name")
    private String skillName;

    @JsonProperty("repository_name")
    private String repositoryName;

    @JsonProperty("repository_url")
    private String repositoryUrl;

    @JsonProperty("content_md")
    private String contentMd;

    private Category category;

    private String summary;

    @JsonProperty("primary_score")
    private float primaryScore;

    private CandidateMetadataDto metadata;
}