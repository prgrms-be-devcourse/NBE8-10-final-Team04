package back.domain.prompt.demo.dto;

import back.domain.prompt.search.enums.QueryType;

public record DemoQuerySearchHit(
        String query,
        QueryType queryType,
        DemoChunkVectorSearchRowDto row
) {
}
