package back.domain.prompt.search.dto.chunk;

import back.domain.prompt.search.dto.candidate.CandidateDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SkillChunkSearchResultDto {
    private List<CandidateDto> candidates;
}
