package back.domain.prompt.search.service;

import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;

import java.util.List;

public interface SkillSearchService {

    SkillChunkSearchResultDto search(List<String> queries);
}
