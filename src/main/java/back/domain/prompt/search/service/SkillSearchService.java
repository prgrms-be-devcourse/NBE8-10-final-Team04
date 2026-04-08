package back.domain.prompt.search.service;

import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;

public interface SkillSearchService {

    SkillChunkSearchResultDto search(String query);
}
