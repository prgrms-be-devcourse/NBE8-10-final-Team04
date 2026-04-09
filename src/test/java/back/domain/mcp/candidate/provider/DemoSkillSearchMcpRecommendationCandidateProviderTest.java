package back.domain.mcp.candidate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.candidate.dto.McpRecommendationQuery;
import back.domain.prompt.demo.service.DemoSkillSearchService;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.search.dto.candidate.CandidateDto;
import back.domain.prompt.search.dto.candidate.CandidateMetadataDto;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;

@ExtendWith(MockitoExtension.class)
class DemoSkillSearchMcpRecommendationCandidateProviderTest {

    @Mock
    private DemoSkillSearchService demoSkillSearchService;

    private DemoSkillSearchMcpRecommendationCandidateProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DemoSkillSearchMcpRecommendationCandidateProvider(demoSkillSearchService);
    }

    @Test
    @DisplayName("데모 스킬 검색 응답을 추천 후보 DTO로 매핑한다")
    void findTopCandidates_mapsSearchResult() {
        CandidateDto candidate = new CandidateDto(
                21L,
                "demo-backend-skill",
                "demo-repo",
                "https://github.com/example/demo-repo",
                Category.BACKEND,
                "summary",
                0.93f,
                new CandidateMetadataDto(120, 15, "2026-03-10T10:00:00Z"));
        when(demoSkillSearchService.search(List.of("SpringBoot", "infra")))
                .thenReturn(new SkillChunkSearchResultDto(List.of(candidate)));

        List<McpRecommendationCandidate> result =
                provider.findTopCandidates(new McpRecommendationQuery(List.of("SpringBoot", "infra")));

        assertThat(result).hasSize(1);
        McpRecommendationCandidate mapped = result.getFirst();
        assertThat(mapped.skillId()).isEqualTo(21L);
        assertThat(mapped.category()).isEqualTo("BACKEND");
        assertThat(mapped.primaryScore()).isCloseTo(0.93, within(0.000001));
        assertThat(mapped.metadata()).isNotNull();
        assertThat(mapped.metadata().stars()).isEqualTo(120);
    }
}
