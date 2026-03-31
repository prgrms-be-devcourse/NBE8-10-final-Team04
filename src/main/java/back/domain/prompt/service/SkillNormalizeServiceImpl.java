package back.domain.prompt.service;

import back.domain.prompt.dto.AgentDto;
import back.domain.prompt.dto.PromptRepoItem;
import back.domain.prompt.dto.RepositoryDto;
import back.domain.prompt.dto.SkillDto;
import back.domain.prompt.entity.Agent;
import back.domain.prompt.entity.Repository;
import back.domain.prompt.entity.Skill;
import back.domain.prompt.enums.Category;
import back.domain.prompt.enums.OwnerType;
import back.domain.prompt.parser.SkillNormalizeParser;
import back.domain.prompt.repository.AgentRepository;
import back.domain.prompt.repository.RepositoryRepository;
import back.domain.prompt.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static back.domain.prompt.enums.Category.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillNormalizeServiceImpl implements SkillNormalizeService {

    private final RepositoryRepository repositoryRepository;
    private final SkillRepository skillRepository;
    private final AgentRepository agentRepository;
    private final SkillNormalizeParser parser;

    @Override
    @Transactional
    public Repository normalizeRepository(PromptRepoItem repoItem) {
        RepositoryDto data = repoItem.getRepository();

        return repositoryRepository.findByGithubId(data.getGithubId())
                .map(existing -> {
                    // getSourceUpatedAt()으로 레포지터리 메타데이터 변경 감지
                    if (!existing.getSourceUpdatedAt().equals(data.getSourceUpdatedAt())) {
                        existing.update(
                                data.getStarCount(),
                                data.getForkCount(),
                                data.getEtag(),
                                data.getSourceUpdatedAt(),
                                data.getSummary(),
                                data.getHomepage(),
                                data.getLicense(),
                                data.getOwnerAvatarUrl(),
                                data.getActive(),
                                data.getRawMetadata(),
                                data.getLanguageStats()
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
                                .githubId(data.getGithubId())
                                .name(data.getName())
                                .sourceRepo(data.getSourceRepo())
                                .sourceUri(data.getSourceUrl())
                                .summary(data.getSummary())
                                .starCount(data.getStarCount())
                                .forkCount(data.getForkCount())
                                .size(data.getSize())
                                .license(data.getLicense())
                                .languageStats(data.getLanguageStats())
                                .homepage(data.getHomepage())
                                .ownerAvatarUrl(data.getOwnerAvatarUrl())
                                .ownerType(data.getOwnerType() != null
                                        ? OwnerType.valueOf(data.getOwnerType().toUpperCase()) : null)
                                .isOfficial(data.getIsOfficial())
                                .defaultBranch(data.getDefaultBranch())
                                .etag(data.getEtag())
                                .sourceUpdatedAt(data.getSourceUpdatedAt())
                                .active(data.getActive())
                                .rawMetadata(data.getRawMetadata() != null ? data.getRawMetadata() : null)
                                .build()
                ));
    }

    @Override
    @Transactional
    public Skill normalizeSkill(Repository repository, SkillDto skillDto) {
        String name = skillDto.getName();
        String summary = repository.getSummary() == null ? "" : repository.getSummary();
        String rawContent = skillDto.getContentMd();
        Set<String> tags = parser.extractTags(summary, rawContent);
        Category category = parser.extractCategory(summary, rawContent);

        return skillRepository.findByRepositoryIdAndName(repository.getId(), name)
                .map(existing -> {
                    if (!existing.getContentHash().equals(skillDto.getContentHash())) {
                        existing.update(rawContent, skillDto.getContentHash(), tags, category);
                        log.info("Skill updated: {}/{}", repository.getSourceRepo(), name);
                    }
                    return existing;
                })
                .orElseGet(() -> skillRepository.save(
                        Skill.builder()
                                .repository(repository)
                                .name(name)
                                .contentMd(rawContent)
                                .contentHash(skillDto.getContentHash())
                                .filePath(skillDto.getFilePath())
                                .category(category)
                                .tagsJson(tags)
                                .build()
                ));
    }

    @Override
    @Transactional
    public Agent normalizeAgent(Repository repository, AgentDto agentDto) {
        String rawContent = agentDto.getContentMd();

        return agentRepository.findByRepositoryId(repository.getId())
                .map(existing -> {
                    if (!existing.getContentHash().equals(agentDto.getContentHash())) {
                        existing.update(rawContent, agentDto.getContentHash());
                        log.info("Agent updated: {}", repository.getSourceRepo());
                    }
                    return existing;
                })
                .orElseGet(() -> agentRepository.save(
                        Agent.builder()
                                .repository(repository)
                                .contentMd(rawContent)
                                .contentHash(agentDto.getContentHash())
                                .filePath(agentDto.getFilePath())
                                .build()
                ));
    }

}
