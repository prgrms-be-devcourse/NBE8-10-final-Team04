package back.domain.prompt.demo.dto;

import back.domain.prompt.prompt.enums.Category;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record DemoSkillRequestDto(

        @JsonProperty("skill_id")
        Long skillId,

        @JsonProperty("skill_name")
        @NotBlank
        String skillName,

        @JsonProperty("repository_name")
        @NotBlank
        String repositoryName,

        @JsonProperty("repository_url")
        @NotBlank
        String repositoryUrl,

        @JsonProperty("summary")
        @NotBlank
        String summary,

        @JsonProperty("content_md")
        @NotBlank
        String contentMd,

        @JsonProperty("category")
        @NotNull
        Category category,

        @JsonProperty("is_chunked")
        Boolean isChunked,

        @JsonProperty("forks")
        Integer forks,

        @JsonProperty("stars")
        Integer stars,

        @JsonProperty("updated_at")
        OffsetDateTime updatedAt,

        @JsonProperty("tags")
        List<String> tags

) {
    public DemoSkillRequestDto {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    @Override
    public List<String> tags() {
        return List.copyOf(tags);
    }

}
