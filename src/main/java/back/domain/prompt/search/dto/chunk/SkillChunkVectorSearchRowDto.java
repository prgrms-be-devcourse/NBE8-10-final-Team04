package back.domain.prompt.search.dto.chunk;

import java.time.LocalDateTime;

public record SkillChunkVectorSearchRowDto(
        Long chunkId,
        Long skillId,
        String skillName,
        String repositoryName,
        String repositoryUrl,
        String contentMd,
        String category,
        String summary,
        Integer stars,
        Integer forks,
        LocalDateTime updatedAt,
        float similarity
) {
}
