package back.domain.prompt.search.dto.search;

import back.domain.prompt.search.dto.chunk.SkillChunkVectorSearchRowDto;
import back.domain.prompt.search.enums.QueryType;

public record QuerySearchHit(
        String query,
        QueryType queryType,
        SkillChunkVectorSearchRowDto row
) {
}
