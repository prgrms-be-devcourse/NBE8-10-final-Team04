package back.domain.prompt.prompt.service;

import back.domain.prompt.prompt.dto.SkillContentDetail;

public interface SkillReadService {
    SkillContentDetail getSkillContent(long skillId);
}

