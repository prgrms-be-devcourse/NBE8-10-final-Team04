package back.domain.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Jackson DTO는 JSON 리스트를 그대로 전달한다.")
public class PromptRepoItem {

    @JsonProperty("repository")
    private RepositoryData repository;

    @JsonProperty("skills")
    private List<SkillData> skills;

    @JsonProperty("agent")
    private AgentData agent;
}
