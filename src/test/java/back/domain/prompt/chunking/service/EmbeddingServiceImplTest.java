package back.domain.prompt.chunking.service;

import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmbeddingServiceImplTest {

    private static final String BASE_URL = "http://localhost";

    private MockRestServiceServer server;
    private EmbeddingServiceImpl embeddingService;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        RestClient restClient = RestClient.builder(restTemplate).baseUrl(BASE_URL).build();
        embeddingService = new EmbeddingServiceImpl(restClient);
    }

    @Test
    @DisplayName("단일 임베딩 요청 시 첫 번째 벡터를 반환한다")
    void embed_returnsFirstEmbedding() {
        server.expect(requestTo(BASE_URL + "/embed"))
                .andRespond(withSuccess("{\"embeddings\":[[0.1,0.2]]}", MediaType.APPLICATION_JSON));

        List<Float> embedding = embeddingService.embed("hello");

        assertThat(embedding).containsExactly(0.1f, 0.2f);
        server.verify();
    }

    @Test
    @DisplayName("단일 임베딩 응답이 비어 있으면 예외를 던진다")
    void embed_throwsWhenResponseIsEmpty() {
        server.expect(requestTo(BASE_URL + "/embed"))
                .andRespond(withSuccess("{\"embeddings\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> embeddingService.embed("hello"))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR));
        server.verify();
    }

    @Test
    @DisplayName("배치 임베딩은 32개 단위로 나눠 여러 응답을 합친다")
    void embedBatch_splitsIntoFixedSizeBatchesAndAggregatesResponses() {
        server.expect(requestTo(BASE_URL + "/embed"))
                .andRespond(withSuccess(jsonForEmbeddings(0, 32), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/embed"))
                .andRespond(withSuccess(jsonForEmbeddings(32, 1), MediaType.APPLICATION_JSON));

        List<List<Float>> embeddings = embeddingService.embedBatch(
                IntStream.range(0, 33).mapToObj(i -> "text-" + i).toList()
        );

        server.verify();
        assertThat(embeddings).hasSize(33);
        assertThat(embeddings.getFirst()).containsExactly(0.0f);
        assertThat(embeddings.get(32)).containsExactly(32.0f);
    }

    @Test
    @DisplayName("배치 임베딩 응답에 embeddings 필드가 없으면 예외를 던진다")
    void embedBatch_throwsWhenEmbeddingsAreMissing() {
        server.expect(requestTo(BASE_URL + "/embed"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> embeddingService.embedBatch(List.of("hello")))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR));
        server.verify();
    }

    private String jsonForEmbeddings(int startInclusive, int count) {
        String embeddings = IntStream.range(startInclusive, startInclusive + count)
                .mapToObj(value -> "[" + value + ".0]")
                .collect(Collectors.joining(","));
        return "{\"embeddings\":[" + embeddings + "]}";
    }
}
