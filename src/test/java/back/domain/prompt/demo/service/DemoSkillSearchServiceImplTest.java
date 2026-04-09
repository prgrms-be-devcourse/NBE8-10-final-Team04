package back.domain.prompt.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import back.domain.prompt.chunking.service.EmbeddingService;
import back.domain.prompt.demo.dto.DemoChunkVectorSearchRowDto;
import back.domain.prompt.demo.repository.DemoSkillChunkVectorSearchRepository;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.provider.QueryTypeRuleProvider;

@ExtendWith(MockitoExtension.class)
class DemoSkillSearchServiceImplTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DemoSkillChunkVectorSearchRepository demoSkillChunkVectorSearchRepository;

    @Mock
    private QueryTypeRuleProvider queryTypeRuleProvider;

    @Test
    @DisplayName("search는 멀티 쿼리를 그룹핑해 점수 순 후보를 반환한다")
    void search_groupsAndSortsCandidates() {
        DemoSkillSearchServiceImpl service = new DemoSkillSearchServiceImpl(
                embeddingService,
                demoSkillChunkVectorSearchRepository,
                queryTypeRuleProvider
        );

        when(queryTypeRuleProvider.getTechQueryWhitelist()).thenReturn(Set.of("spring boot"));
        when(queryTypeRuleProvider.getFunctionHintWords()).thenReturn(Set.of("login"));
        when(queryTypeRuleProvider.getAliasGroups()).thenReturn(Map.of(
                "spring boot", List.of("springboot", "spring boot")
        ));

        when(embeddingService.embed("spring boot")).thenReturn(List.of(0.1f, 0.2f));
        when(embeddingService.embed("login")).thenReturn(List.of(0.3f, 0.4f));

        when(demoSkillChunkVectorSearchRepository.searchAll("[0.1,0.2]")).thenReturn(List.of(
                row(1L, 101L, "spring boot api", "BACKEND", "spring boot login", 0.80f),
                row(2L, 102L, "node service", "BACKEND", "payment", 0.60f)
        ));
        when(demoSkillChunkVectorSearchRepository.searchAll("[0.3,0.4]")).thenReturn(List.of(
                row(3L, 101L, "spring boot api", "BACKEND", "spring boot login", 0.77f),
                row(4L, 102L, "node service", "BACKEND", "payment", 0.58f)
        ));

        SkillChunkSearchResultDto result = service.search(List.of("spring boot", "login"));

        verify(demoSkillChunkVectorSearchRepository).searchAll("[0.1,0.2]");
        verify(demoSkillChunkVectorSearchRepository).searchAll("[0.3,0.4]");
        assertThat(result.candidates()).hasSize(2);
        assertThat(result.candidates().get(0).skillId()).isEqualTo(101L);
        assertThat(result.candidates().get(1).skillId()).isEqualTo(102L);
    }

    @Test
    @DisplayName("search는 유효한 질의가 없으면 예외를 던진다")
    void search_throwsWhenNoQueries() {
        DemoSkillSearchServiceImpl service = new DemoSkillSearchServiceImpl(
                embeddingService,
                demoSkillChunkVectorSearchRepository,
                queryTypeRuleProvider
        );

        assertThatThrownBy(() -> service.search(List.of("   ", "")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DemoChunkVectorSearchRowDto row(
            Long chunkId,
            Long demoSkillId,
            String skillName,
            String category,
            String summary,
            float similarity
    ) {
        return new DemoChunkVectorSearchRowDto(
                chunkId,
                demoSkillId,
                skillName,
                "demo-repo",
                "https://example.com/" + skillName.replace(" ", "-"),
                category,
                summary,
                10,
                2,
                OffsetDateTime.parse("2026-04-09T00:00:00Z"),
                similarity
        );
    }
}
