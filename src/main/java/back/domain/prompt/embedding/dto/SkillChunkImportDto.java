package back.domain.prompt.embedding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class SkillChunkImportDto {

    @JsonProperty("skill_id")
    private Long skillId;

    @JsonProperty("chunk_index")
    private Integer chunkIndex;

    @JsonProperty("section_title")
    private String sectionTitle;

    @JsonProperty("section_path")
    private String sectionPath;

//    @JsonProperty("content_md")
//    private String contentMd;

    @JsonProperty("search_text")
    private String searchText;

//    @JsonProperty("preview_text")
//    private String previewText;

    @JsonProperty("char_count")
    private Integer charCount;

    @JsonProperty("chunk_version")
    private String chunkVersion;

    @JsonProperty("embedding_model")
    private String embeddingModel;

    @JsonProperty("embedded_at")
    private OffsetDateTime embeddedAt;

    @JsonProperty("embedding")
    private List<Float> embedding;
}
