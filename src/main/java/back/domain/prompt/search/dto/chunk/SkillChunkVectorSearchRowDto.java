package back.domain.prompt.search.dto.chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SkillChunkVectorSearchRowDto {
    private Long chunkId;
    private Long skillId;
    private String skillName;
    private String repositoryName;
    private String repositoryUrl;
    private String summary;
    private Integer stars;
    private Integer forks;
    private LocalDateTime updatedAt;
    private String previewText;
    private float similarity;
}
