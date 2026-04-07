package back.domain.prompt.search.dto.chunk;

import back.domain.prompt.search.dto.candidate.CandidateDto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "응답 DTO로 직렬화를 위해 구성된 후보 목록을 그대로 노출한다."
)
public record SkillChunkSearchResultDto(
        List<CandidateDto> candidates
) {
}
