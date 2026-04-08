package back.domain.prompt.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Jackson DTO 필드는 역직렬화된 값을 그대로 전송용으로 노출한다."
)
public record PromptRepoItem(
        @JsonProperty("repository")
        RepositoryDto repository,

        @JsonProperty("skills")
        List<SkillDto> skills,

        @JsonProperty("agent")
        AgentDto agent
) {
}
