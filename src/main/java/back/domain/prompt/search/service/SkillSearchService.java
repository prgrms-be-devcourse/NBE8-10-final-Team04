package back.domain.prompt.search.service;

import java.util.Comparator;
import java.util.stream.Collectors;

import back.domain.prompt.embedding.service.EmbeddingService;
import back.domain.prompt.search.dto.candidate.CandidateDto;
import back.domain.prompt.search.dto.candidate.CandidateMetadataDto;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.dto.chunk.SkillChunkVectorSearchRowDto;
import back.domain.prompt.search.repository.SkillChunkVectorSearchRepository;
import back.domain.prompt.search.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillSearchService {

    private final EmbeddingService embeddingService;
    private final SkillChunkVectorSearchRepository skillChunkVectorSearchRepository;

    public SkillChunkSearchResultDto search(String query, int topK) {
        List<Float> queryEmbedding = embeddingService.embed(query);
        String queryVector = VectorUtils.toPgVector(queryEmbedding);

        List<SkillChunkVectorSearchRowDto> results =
                skillChunkVectorSearchRepository.searchTopK(queryVector, topK * 3);

        List<CandidateDto> candidates = results.stream()
                .collect(Collectors.groupingBy(SkillChunkVectorSearchRowDto::getSkillId))
                .values().stream()
                .map(this::toGroupedCandidateDto)
                .sorted(Comparator.comparing(CandidateDto::getPrimaryScore).reversed())
                .limit(topK)
                .toList();

        return new SkillChunkSearchResultDto(candidates);
    }

    private CandidateDto toGroupedCandidateDto(List<SkillChunkVectorSearchRowDto> groupedResults) {
        SkillChunkVectorSearchRowDto best = groupedResults.stream()
                .max(Comparator.comparing(SkillChunkVectorSearchRowDto::getSimilarity))
                .orElseThrow();

        return new CandidateDto(
                best.getSkillId(),
                best.getSkillName(),
                best.getRepositoryName(),
                best.getRepositoryUrl(),
                buildSummary(best),
                best.getSimilarity(),
                new CandidateMetadataDto(
                        best.getStars(),
                        best.getForks(),
                        best.getUpdatedAt() != null ? best.getUpdatedAt().toString() : null
                )
        );
    }

    private String buildSummary(SkillChunkVectorSearchRowDto result) {
        if (result.getSummary() != null && !result.getSummary().isBlank()) {
            return result.getSummary();
        }
        return result.getPreviewText();
    }
}
