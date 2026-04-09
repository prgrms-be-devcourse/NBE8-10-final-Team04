package back.domain.prompt.demo.service;

import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;

import java.util.List;

public interface DemoSkillSearchService {

    SkillChunkSearchResultDto search(List<String> queries);
}
