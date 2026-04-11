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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillUpsertServiceImpl implements SkillUpsertService {

    private static final int SKILL_INSERT_BATCH_SIZE = 200;

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
    public void upsertSkills(Repository repository, List<SkillDto> skillDtos) {
        // 계산(파싱)과 저장을 분리해서 트랜잭션 점유 시간을 줄인다.
        if (skillDtos == null || skillDtos.isEmpty()) {
            log.warn("[SkillUpsertServiceImpl#upsertSkills] 스킬 목록이 비어 있습니다. repo={}", repository.getSourceRepo());
            return;
        }

        String summary = repository.getSummary() == null ? "" : repository.getSummary();
        Map<String, Skill> existingSkillsByName = skillRepository.findByRepositoryId(repository.getId())
                .stream()
                .collect(Collectors.toMap(Skill::getName, Function.identity(), (left, right) -> left));

        List<Skill> newSkills = new ArrayList<>();
        List<Skill> changedSkills = new ArrayList<>();

        for (SkillDto skillDto : skillDtos) {
            if (skillDto == null) {
                continue;
            }

            if (skillDto.contentMd() == null) {
                log.warn("[SkillUpsertServiceImpl#upsertSkills] skill content_md가 없습니다. {}/{}",
                        repository.getSourceRepo(), skillDto.name());
                continue;
            }

            String name = skillDto.name();
            String rawContent = skillDto.contentMd();
            Set<String> tags = parser.extractTags(summary, rawContent);
            Category category = parser.extractCategory(summary, rawContent);

            Skill existing = existingSkillsByName.get(name);
            if (existing != null) {
                if (!Objects.equals(existing.getContentHash(), skillDto.contentHash())) {
                    existing.update(rawContent, skillDto.contentHash(), tags, category);
                    changedSkills.add(existing);
                    log.info("[SkillUpsertServiceImpl#upsertSkills] 스킬 갱신 완료: {}/{}", repository.getSourceRepo(), name);
                }
                continue;
            }

            Skill created = Skill.builder()
                    .repository(repository)
                    .name(name)
                    .contentMd(rawContent)
                    .contentHash(skillDto.contentHash())
                    .filePath(skillDto.filePath())
                    .category(category)
                    .tagsJson(tags)
                    .build();
            newSkills.add(created);
            existingSkillsByName.put(name, created);
        }

        if (!newSkills.isEmpty()) {
            saveSkillsInBatches(repository, newSkills, "신규");
        }

        if (!changedSkills.isEmpty()) {
            saveSkillsInBatches(repository, changedSkills, "수정");
        }
    }

    private void saveSkillsInBatches(Repository repository, List<Skill> skills, String mode) {
        // 배치 저장 실패 시 개별 저장으로 폴백해 유효한 스킬 처리를 계속 진행한다.
        int total = skills.size();
        for (int from = 0; from < total; from += SKILL_INSERT_BATCH_SIZE) {
            int to = Math.min(from + SKILL_INSERT_BATCH_SIZE, total);
            List<Skill> batch = skills.subList(from, to);

            try {
                skillRepository.saveAll(batch);
                log.info(
                        "[SkillUpsertServiceImpl#upsertSkills] {} 스킬 배치 저장 완료. repo={}, batchSize={}, progress={}/{}",
                        mode,
                        repository.getSourceRepo(),
                        batch.size(),
                        to,
                        total
                );
            } catch (Exception e) {
                log.error(
                        "[SkillUpsertServiceImpl#upsertSkills] {} 스킬 배치 저장 실패. 개별 저장으로 재시도합니다. repo={}, batchSize={}, progress={}/{}",
                        mode,
                        repository.getSourceRepo(),
                        batch.size(),
                        to,
                        total,
                        e
                );
                saveSkillsIndividually(repository, batch, mode);
            }
        }
    }

    private void saveSkillsIndividually(Repository repository, List<Skill> batch, String mode) {
        for (Skill skill : batch) {
            try {
                skillRepository.save(skill);
            } catch (Exception e) {
                log.error(
                        "[SkillUpsertServiceImpl#upsertSkills] {} 스킬 개별 저장 실패. repo={}, skillName={}, filePath={}",
                        mode,
                        repository.getSourceRepo(),
                        skill.getName(),
                        skill.getFilePath(),
                        e
                );
            }
        }
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
                        log.info("[SkillUpsertServiceImpl#upsertSkill] 스킬 갱신 완료: {}/{}", repository.getSourceRepo(), name);
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
                        log.info("[SkillUpsertServiceImpl#upsertAgent] 에이전트 갱신 완료: {}", repository.getSourceRepo());
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