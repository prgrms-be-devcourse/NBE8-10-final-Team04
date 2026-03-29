package back.domain.prompt.embedding.dto;

import java.util.List;

public record EmbeddingRequest(
        List<String> texts
) {
}
