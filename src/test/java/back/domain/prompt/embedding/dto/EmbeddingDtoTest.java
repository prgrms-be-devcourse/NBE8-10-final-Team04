package back.domain.prompt.embedding.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmbeddingDtoTest {

    @Test
    @DisplayName("EmbeddingRequest는 texts를 보관한다")
    void embeddingRequest_keepsTexts() {
        EmbeddingRequest request = new EmbeddingRequest(List.of("alpha", "beta"));

        assertThat(request.texts()).containsExactly("alpha", "beta");
    }

    @Test
    @DisplayName("EmbeddingResponse는 embeddings를 보관한다")
    void embeddingResponse_keepsEmbeddings() {
        EmbeddingResponse response = new EmbeddingResponse(List.of(List.of(0.1f, 0.2f)));

        assertThat(response.embeddings()).containsExactly(List.of(0.1f, 0.2f));
    }
}
