package back.domain.prompt.search.dto.search;

import back.domain.prompt.search.enums.QueryType;

public record SearchQueryDto(
        String text,
        QueryType type
) {
}
