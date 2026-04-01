package back.domain.prompt.embedding.service;

import back.domain.prompt.embedding.dto.EmbeddingRequest;
import back.domain.prompt.embedding.dto.EmbeddingResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "공유 WebClient 빈은 스프링이 주입하고 관리한다."
)
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
