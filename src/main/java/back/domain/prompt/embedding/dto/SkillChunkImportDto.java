package back.domain.prompt.embedding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Jackson DTO로 파싱한 임베딩 값을 방어적 복사 없이 그대로 전달한다."
)
public class SkillChunkImportDto {

    @JsonProperty("skill_id")
    private Long skillId;

    @JsonProperty("chunk_index")
    private Integer chunkIndex;

    @JsonProperty("section_title")
    private String sectionTitle;

    @JsonProperty("section_path")
    private String sectionPath;

    @JsonProperty("search_text")
    private String searchText;

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
