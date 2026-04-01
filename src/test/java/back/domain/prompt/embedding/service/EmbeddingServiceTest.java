package back.domain.prompt.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

class EmbeddingServiceTest {

    @Test
    @DisplayName("embed는 첫 번째 임베딩 벡터를 반환한다")
    void embed_returnsFirstEmbedding() {
        EmbeddingService embeddingService = new EmbeddingService(webClientWithJson("""
                {
                  "embeddings": [
                    [0.1, 0.2],
                    [9.9]
                  ]
                }
                """));

        List<Float> embedding = embeddingService.embed("spring");

        assertThat(embedding).containsExactly(0.1f, 0.2f);
    }

    @Test
    @DisplayName("embed는 비어 있는 응답이면 예외를 던진다")
    void embed_throwsWhenResponseIsEmpty() {
        EmbeddingService embeddingService = new EmbeddingService(webClientWithJson("""
                {
                  "embeddings": []
                }
                """));

        assertThatThrownBy(() -> embeddingService.embed("spring"))
                .isInstanceOf(IllegalStateException.class);
    }

    private WebClient webClientWithJson(String body) {
        ExchangeFunction exchangeFunction = request -> {
            assertThat(request.url().getPath()).isEqualTo("/embed");
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        };

        return WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
    }
}
