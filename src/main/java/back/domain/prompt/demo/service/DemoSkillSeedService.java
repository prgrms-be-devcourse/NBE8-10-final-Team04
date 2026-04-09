package back.domain.prompt.demo.service;

import back.domain.prompt.demo.dto.DemoSkillRequestDto;
import back.domain.prompt.demo.entity.DemoSkill;
import back.domain.prompt.demo.repository.DemoSkillRepository;
import back.domain.prompt.prompt.parser.SkillNormalizeParser;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링 DI로 주입되는 공유 의존성(ObjectMapper 포함)을 서비스 필드로 보관합니다."
)
public class DemoSkillSeedService {

    private static final String MOCK_FILE_PATH = "mock/skill-mock.json";

    private final DemoSkillRepository demoSkillRepository;
    private final DemoChunkingProcessor demoChunkingProcessor;
    private final SkillNormalizeParser skillNormalizeParser;
    private final ObjectMapper objectMapper;

    // skill-mock.json을 읽어 demo_skills에 저장하고 청킹·임베딩까지 수행한다.
    // 이미 데이터가 존재하면 건너뛴다.
    public void seed() {
        if (demoSkillRepository.count() > 0) {
            log.info("demo_skills 데이터가 이미 존재합니다. 시드를 건너뜁니다.");
            return;
        }

        List<DemoSkillRequestDto> dtos = loadFromJson();
        log.info("skill-mock.json에서 {}개 항목을 로드했습니다.", dtos.size());

        int success = 0;
        for (DemoSkillRequestDto dto : dtos) {
            try {
                DemoSkill saved = saveSkill(dto);
                demoChunkingProcessor.processOne(saved);
                success++;
            } catch (Exception e) {
                log.error("데모 스킬 처리 실패 - skillId={}, name={}: {}", dto.skillId(), dto.skillName(), e.getMessage());
            }
        }

        log.info("데모 스킬 시드 완료: {}/{}", success, dtos.size());
    }

    // 단일 DemoSkill 저장 (트랜잭션 분리 → 청킹 실패 시 저장 데이터 유지)
    @Transactional
    public DemoSkill saveSkill(DemoSkillRequestDto dto) {
        Set<String> tags = skillNormalizeParser.extractTags(dto.summary(), dto.contentMd());
        List<String> aliases = skillNormalizeParser.extractAliases(dto.summary(), dto.contentMd());

        DemoSkill skill = DemoSkill.builder()
                .skillId(dto.skillId())
                .skillName(dto.skillName())
                .repositoryName(dto.repositoryName())
                .repositoryUrl(dto.repositoryUrl())
                .summary(dto.summary())
                .contentMd(dto.contentMd())
                .category(dto.category())
                .isChunked(false)
                .forks(dto.forks() != null ? dto.forks() : 0)
                .stars(dto.stars() != null ? dto.stars() : 0)
                .sourceUpdatedAt(dto.updatedAt())
                .tags(tags)
                .aliases(aliases)
                .build();

        return demoSkillRepository.save(skill);
    }

    private List<DemoSkillRequestDto> loadFromJson() {
        try {
            InputStream is = new ClassPathResource(MOCK_FILE_PATH).getInputStream();
            SkillMockWrapper wrapper = objectMapper.readValue(is, SkillMockWrapper.class);
            return wrapper.skills();
        } catch (IOException e) {
            throw new IllegalStateException("skill-mock.json 파일을 읽는 데 실패했습니다: " + MOCK_FILE_PATH, e);
        }
    }

    private record SkillMockWrapper(@JsonProperty("skills") List<DemoSkillRequestDto> skills) {}
}
