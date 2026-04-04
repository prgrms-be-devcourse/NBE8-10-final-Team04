package back.domain.prompt.prompt.service;

import back.domain.prompt.prompt.dto.AgentDto;
import back.domain.prompt.prompt.dto.PromptRepoItem;
import back.domain.prompt.prompt.dto.RepositoryDto;
import back.domain.prompt.prompt.dto.SkillDto;
import back.domain.prompt.prompt.entity.Agent;
import back.domain.prompt.prompt.entity.Repository;
import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.enums.OwnerType;
import back.domain.prompt.prompt.parser.SkillNormalizeParser;
import back.domain.prompt.prompt.repository.AgentRepository;
import back.domain.prompt.prompt.repository.RepositoryRepository;
import back.domain.prompt.prompt.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillUpsertServiceImpl implements SkillUpsertService {

    private final RepositoryRepository repositoryRepository;
    private final SkillRepository skillRepository;
    private final AgentRepository agentRepository;
    private final SkillNormalizeParser parser;

    @Override
    @Transactional
    public Repository upsertRepository(PromptRepoItem repoItem) {
        RepositoryDto data = repoItem.repository();

        return repositoryRepository.findByGithubId(data.githubId())
                .map(existing -> {
                    // getSourceUpatedAt()으로 레포지터리 메타데이터 변경 감지
                    if (!existing.getSourceUpdatedAt().equals(data.sourceUpdatedAt())) {
                        existing.update(
                                data.starCount(),
                                data.forkCount(),
                                data.etag(),
                                data.sourceUpdatedAt(),
                                data.summary(),
                                data.homepage(),
                                data.license(),
                                data.ownerAvatarUrl(),
                                data.active(),
                                data.rawMetadata(),
                                data.languageStats()
                        );

                        // 레포 메타데이터 변경 -> skills tag와 category 업데이트
                        existing.getSkills().forEach(skill -> {
                            String summary = existing.getSummary() == null ? "" : existing.getSummary();

                            Set<String> tags = parser.extractTags(summary, skill.getContentMd());
                            Category category = parser.extractCategory(summary, skill.getContentMd());

                            skill.updateTagAndCategory(tags, category);
                        });

                    }
                    return existing;
                })
                .orElseGet(() -> repositoryRepository.save(
                        Repository.builder()
                                .githubId(data.githubId())
                                .name(data.name())
                                .sourceRepo(data.sourceRepo())
                                .sourceUri(data.sourceUrl())
                                .summary(data.summary())
                                .starCount(data.starCount())
                                .forkCount(data.forkCount())
                                .size(data.size())
                                .license(data.license())
                                .languageStats(data.languageStats())
                                .homepage(data.homepage())
                                .ownerAvatarUrl(data.ownerAvatarUrl())
                                .ownerType(data.ownerType() != null
                                        ? OwnerType.valueOf(data.ownerType().toUpperCase()) : null)
                                .isOfficial(data.isOfficial())
                                .defaultBranch(data.defaultBranch())
                                .etag(data.etag())
                                .sourceUpdatedAt(data.sourceUpdatedAt())
                                .active(data.active())
                                .rawMetadata(data.rawMetadata() != null ? data.rawMetadata() : null)
                                .build()
                ));
    }

    @Override
    @Transactional
    public Skill upsertSkill(Repository repository, SkillDto skillDto) {
        String name = skillDto.name();
        String summary = repository.getSummary() == null ? "" : repository.getSummary();
        String rawContent = skillDto.contentMd();
        Set<String> tags = parser.extractTags(summary, rawContent);
        Category category = parser.extractCategory(summary, rawContent);

        return skillRepository.findByRepositoryIdAndName(repository.getId(), name)
                .map(existing -> {
                    if (!existing.getContentHash().equals(skillDto.contentHash())) {
                        existing.update(rawContent, skillDto.contentHash(), tags, category);
                        log.info("[SkillUpsertServiceImpl#upsertSkill] Skill updated: {}/{}", repository.getSourceRepo(), name);
                    }
                    return existing;
                })
                .orElseGet(() -> skillRepository.save(
                        Skill.builder()
                                .repository(repository)
                                .name(name)
                                .contentMd(rawContent)
                                .contentHash(skillDto.contentHash())
                                .filePath(skillDto.filePath())
                                .category(category)
                                .tagsJson(tags)
                                .build()
                ));
    }

    @Override
    @Transactional
    public Agent upsertAgent(Repository repository, AgentDto agentDto) {
        String rawContent = agentDto.contentMd();

        return agentRepository.findByRepositoryId(repository.getId())
                .map(existing -> {
                    if (!existing.getContentHash().equals(agentDto.contentHash())) {
                        existing.update(rawContent, agentDto.contentHash());
                        log.info("[SkillUpsertServiceImpl#upsertAgent] Agent updated: {}", repository.getSourceRepo());
                    }
                    return existing;
                })
                .orElseGet(() -> agentRepository.save(
                        Agent.builder()
                                .repository(repository)
                                .contentMd(rawContent)
                                .contentHash(agentDto.contentHash())
                                .filePath(agentDto.filePath())
                                .build()
                ));
    }

}
