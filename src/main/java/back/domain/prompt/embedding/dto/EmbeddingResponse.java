package back.domain.prompt.embedding.dto;

import java.util.List;

public record EmbeddingResponse(
        List<List<Float>> embeddings
) {
}
