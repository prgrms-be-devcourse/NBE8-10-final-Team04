package back.domain.prompt.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.prompt.demo.dto.DemoSkillRequestDto;
import back.domain.prompt.demo.entity.DemoSkill;
import back.domain.prompt.demo.repository.DemoSkillRepository;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.prompt.parser.SkillNormalizeParser;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DemoSkillSeedServiceTest {

    @Mock
    private DemoSkillRepository demoSkillRepository;

    @Mock
    private DemoChunkingProcessor demoChunkingProcessor;

    @Mock
    private SkillNormalizeParser skillNormalizeParser;

    @Test
    @DisplayName("saveSkill은 태그/별칭을 추출하고 기본값을 반영해 저장한다")
    void saveSkill_buildsAndSavesEntity() {
        DemoSkillSeedService service = new DemoSkillSeedService(
                demoSkillRepository,
                demoChunkingProcessor,
                skillNormalizeParser,
                new ObjectMapper()
        );

        DemoSkillRequestDto dto = new DemoSkillRequestDto(
                1L,
                "springboot-patterns",
                "demo-repo",
                "https://example.com/repo",
                "summary",
                "content",
                Category.BACKEND,
                false,
                null,
                null,
                OffsetDateTime.parse("2026-04-09T00:00:00Z"),
                List.of(),
                List.of()
        );

        when(skillNormalizeParser.extractTags("summary", "content")).thenReturn(Set.of("spring"));
        when(skillNormalizeParser.extractAliases("summary", "content")).thenReturn(List.of("springboot"));
        when(demoSkillRepository.save(any(DemoSkill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DemoSkill saved = service.saveSkill(dto);

        assertThat(saved.getSkillId()).isEqualTo(1L);
        assertThat(saved.getForks()).isEqualTo(0);
        assertThat(saved.getStars()).isEqualTo(0);
        assertThat(saved.getTags()).containsExactly("spring");
        assertThat(saved.getAliases()).containsExactly("springboot");
    }

    @Test
    @DisplayName("seed는 데이터가 이미 있으면 시딩을 건너뛴다")
    void seed_skipsWhenAlreadySeeded() {
        DemoSkillSeedService service = new DemoSkillSeedService(
                demoSkillRepository,
                demoChunkingProcessor,
                skillNormalizeParser,
                new ObjectMapper()
        );

        when(demoSkillRepository.count()).thenReturn(1L);

        service.seed();

        verify(demoChunkingProcessor, never()).processOne(any());
    }

    @Test
    @DisplayName("seed는 mock 파일을 읽어 저장과 청킹을 수행한다")
    void seed_loadsJsonAndProcessesSkills() {
        DemoSkillSeedService service = new DemoSkillSeedService(
                demoSkillRepository,
                demoChunkingProcessor,
                skillNormalizeParser,
                new ObjectMapper()
        );

        when(demoSkillRepository.count()).thenReturn(0L);
        when(skillNormalizeParser.extractTags(any(), any())).thenReturn(Set.of("tag"));
        when(skillNormalizeParser.extractAliases(any(), any())).thenReturn(List.of("alias"));
        when(demoSkillRepository.save(any(DemoSkill.class))).thenAnswer(invocation -> {
            DemoSkill skill = invocation.getArgument(0);
            ReflectionTestUtils.setField(skill, "id", 999L);
            return skill;
        });

        service.seed();

        verify(demoSkillRepository).count();
        verify(demoSkillRepository, org.mockito.Mockito.atLeastOnce()).save(any(DemoSkill.class));
        verify(demoChunkingProcessor, org.mockito.Mockito.atLeastOnce()).processOne(any(DemoSkill.class));
    }

    @Test
    @DisplayName("SkillMockWrapper는 skills 목록을 보관한다")
    void skillMockWrapper_storesSkills() throws Exception {
        DemoSkillRequestDto dto = new DemoSkillRequestDto(
                1L, "skill", "repo", "url", "summary", "content",
                Category.BACKEND, false, 0, 0, null, List.of(), List.of()
        );

        Class<?> wrapperClass = null;
        for (Class<?> inner : DemoSkillSeedService.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("SkillMockWrapper")) {
                wrapperClass = inner;
                break;
            }
        }

        assertThat(wrapperClass).isNotNull();
        Constructor<?> constructor = wrapperClass.getDeclaredConstructor(List.class);
        constructor.setAccessible(true);
        Object wrapper = constructor.newInstance(List.of(dto));

        Method skillsMethod = wrapperClass.getDeclaredMethod("skills");
        skillsMethod.setAccessible(true);
        Object value = skillsMethod.invoke(wrapper);

        @SuppressWarnings("unchecked")
        List<DemoSkillRequestDto> skills = (List<DemoSkillRequestDto>) value;
        assertThat(skills).hasSize(1);
        assertThat(skills.getFirst().skillName()).isEqualTo("skill");
    }
}
