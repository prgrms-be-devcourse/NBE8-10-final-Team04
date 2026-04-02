package back.domain.prompt.prompt.service;

import back.domain.prompt.prompt.dto.AgentDto;
import back.domain.prompt.prompt.dto.PromptRepoItem;
import back.domain.prompt.prompt.dto.SkillDto;
import back.domain.prompt.prompt.entity.Agent;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;

public interface SkillNormalizeService {

    Repository normalizeRepository(PromptRepoItem repoItem);

    Skill normalizeSkill(Repository repository, SkillDto skillDto);

    Agent normalizeAgent(Repository repository, AgentDto agentDto);

}
