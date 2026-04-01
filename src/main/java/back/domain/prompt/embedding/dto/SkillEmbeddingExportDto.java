package back.domain.prompt.embedding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SkillEmbeddingExportDto {

    @JsonProperty("skill_id")
    private Long skillId;

    @JsonProperty("repository_id")
    private Long repositoryId;

    private String name;

    private String path;

    @JsonProperty("content_md")
    private String contentMd;

    @JsonProperty("content_hash")
    private String contentHash;
}
