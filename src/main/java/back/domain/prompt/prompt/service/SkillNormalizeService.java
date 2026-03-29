package back.domain.prompt.service;

import back.domain.prompt.dto.AgentDto;
import back.domain.prompt.dto.PromptRepoItem;
import back.domain.prompt.dto.SkillDto;
import back.domain.prompt.entity.Agent;
import back.domain.prompt.entity.Repository;
import back.domain.prompt.entity.Skill;

import java.util.List;

public interface SkillNormalizeService {

    Repository normalizeRepository(PromptRepoItem repoItem);

    Skill normalizeSkill(Repository repository, SkillDto skillDto);

    Agent normalizeAgent(Repository repository, AgentDto agentDto);

    List<String> extractTags(String content);
}
