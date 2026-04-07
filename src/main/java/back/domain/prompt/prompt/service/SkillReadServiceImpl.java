package back.domain.prompt.prompt.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import back.domain.prompt.prompt.dto.SkillContentDetail;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.repository.SkillRepository;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SkillReadServiceImpl implements SkillReadService {
    private static final String SKILL_NOT_FOUND_MESSAGE = "스킬이 존재하지 않습니다.";

    private final SkillRepository skillRepository;

    @Override
    @Transactional(readOnly = true)
    public SkillContentDetail getSkillContent(long skillId) {
        Skill skill = skillRepository.findByIdWithRepository(skillId).orElseThrow(() -> new ServiceException(
                CommonErrorCode.NOT_FOUND,
                "[SkillReadServiceImpl#getSkillContent] skill not found. skillId=%d".formatted(skillId),
                SKILL_NOT_FOUND_MESSAGE));

        return new SkillContentDetail(
                skill.getId(),
                skill.getCategory().name().toLowerCase(),
                skill.getRepository().getSourceRepo(),
                skill.getContentMd());
    }
}

