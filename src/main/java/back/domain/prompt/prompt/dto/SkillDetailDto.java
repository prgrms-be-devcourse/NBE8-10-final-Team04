package back.domain.prompt.prompt.dto;

import back.domain.prompt.prompt.entity.Skill;

import java.util.Set;

public record SkillDetailDto(
        long id,
        String name,
        String category,
        Set<String> tags,
        String repositoryName,
        String repositoryUrl,
        String summary,
        Integer stars,
        Integer forks,
        String contentMd
) {
    public SkillDetailDto {
        tags = (tags == null) ? Set.of() : Set.copyOf(tags);
    }

    public static SkillDetailDto from(Skill skill) {
        var repo = skill.getRepository();
        return new SkillDetailDto(
                skill.getId(),
                skill.getName(),
                skill.getCategory().name(),
                skill.getTagsJson(),
                repo.getName(),
                repo.getSourceUri(),
                repo.getSummary(),
                repo.getStarCount(),
                repo.getForkCount(),
                skill.getContentMd()
        );
    }
}
