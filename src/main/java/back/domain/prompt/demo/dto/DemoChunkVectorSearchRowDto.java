package back.domain.prompt.demo.dto;

import java.time.OffsetDateTime;

public record DemoChunkVectorSearchRowDto(
        Long chunkId,
        Long demoSkillId,
        String skillName,
        String repositoryName,
        String repositoryUrl,
        String category,
        String summary,
        Integer stars,
        Integer forks,
        OffsetDateTime updatedAt,
        float similarity,
        float boostScore
) {
}
