package back.domain.prompt.embedding.service;

import back.domain.prompt.embedding.dto.EmbeddingRequest;
import back.domain.prompt.embedding.dto.EmbeddingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final WebClient embeddingWebClient;

    public List<Float> embed(String text) {
        EmbeddingResponse response = embeddingWebClient.post()
                .uri("/embed")
                .bodyValue(new EmbeddingRequest(List.of(text)))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();

        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new IllegalStateException("임베딩 응답이 비어 있습니다.");
        }

        return response.embeddings().getFirst();
    }
}
