package back.domain.prompt.search.dto.request;

import java.util.List;

public record SkillSearchRequestDto(
        List<String> queries
) {
    public SkillSearchRequestDto {
        queries = queries == null ? List.of() : List.copyOf(queries);
    }

    @Override
    public List<String> queries() {
        return List.copyOf(queries);
    }
}
