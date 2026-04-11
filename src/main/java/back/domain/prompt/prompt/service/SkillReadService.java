package back.domain.prompt.prompt.service;

import back.domain.prompt.prompt.dto.SkillContentDetail;
import back.domain.prompt.prompt.dto.SkillDetailDto;
import back.domain.prompt.prompt.dto.SkillListItemDto;
import back.domain.prompt.prompt.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SkillReadService {
    SkillContentDetail getSkillContent(long skillId);

    Page<SkillListItemDto> getSkills(Pageable pageable, Category category);

    Page<SkillListItemDto> searchSkills(String keyword, Pageable pageable);

    SkillDetailDto getSkillDetail(long skillId);
}

