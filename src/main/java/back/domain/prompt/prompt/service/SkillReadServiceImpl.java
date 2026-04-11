package back.domain.prompt.prompt.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import back.domain.prompt.prompt.dto.SkillContentDetail;
import back.domain.prompt.prompt.dto.SkillDetailDto;
import back.domain.prompt.prompt.dto.SkillListItemDto;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
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

    @Override
    @Transactional(readOnly = true)
    public Page<SkillListItemDto> getSkills(Pageable pageable, Category category) {
        Page<Skill> page = (category == null)
                ? skillRepository.findAllWithRepository(pageable)
                : skillRepository.findByCategoryWithRepository(category, pageable);
        return page.map(SkillListItemDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SkillListItemDto> searchSkills(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return skillRepository.findAllWithRepository(pageable).map(SkillListItemDto::from);
        }
        List<Long> allIds = skillRepository.searchIdsByKeyword("%" + keyword.trim() + "%");
        if (allIds.isEmpty()) {
            return Page.empty(pageable);
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allIds.size());
        List<Long> pageIds = allIds.subList(start, end);
        Map<Long, Skill> skillMap = skillRepository.findAllByIdsWithRepository(pageIds).stream()
                .collect(Collectors.toMap(Skill::getId, s -> s));
        List<SkillListItemDto> dtos = pageIds.stream()
                .map(skillMap::get)
                .map(SkillListItemDto::from)
                .toList();
        return new PageImpl<>(dtos, pageable, allIds.size());
    }

    @Override
    @Transactional(readOnly = true)
    public SkillDetailDto getSkillDetail(long skillId) {
        Skill skill = skillRepository.findByIdWithRepository(skillId)
                .orElseThrow(() -> new ServiceException(
                        CommonErrorCode.NOT_FOUND,
                        "[SkillReadServiceImpl#getSkillDetail] skill not found. skillId=%d".formatted(skillId),
                        SKILL_NOT_FOUND_MESSAGE));
        return SkillDetailDto.from(skill);
    }
}

