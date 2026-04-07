package back.domain.prompt.prompt.dto;

public record SkillContentDetail(
        long skillId,
        String category,
        String sourceRepo,
        String contentMd) {}

