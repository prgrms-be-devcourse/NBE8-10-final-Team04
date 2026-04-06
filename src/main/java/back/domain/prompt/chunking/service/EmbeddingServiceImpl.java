package back.domain.prompt.chunking.service;

import back.domain.prompt.chunking.dto.EmbeddingRequest;
import back.domain.prompt.chunking.dto.EmbeddingResponse;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final String EMBEDDING_RESPONSE_EMPTY = "임베딩 응답이 비어 있습니다.";
    private static final String EMBEDDING_SIZE_MISMATCH = "임베딩 응답 크기가 요청된 배치 크기와 일치하지 않습니다.";

    private final RestClient embeddingRestClient;

    // 배치 사이즈는 Fast API에 있는 배치 사이즈와 통일시켜야 한다.
    private static final int BATCH_SIZE = 32;

    public List<Float> embed(String text) {
        EmbeddingResponse response = embeddingRestClient.post()
                .uri("/embed")
                .body(new EmbeddingRequest(List.of(text)))
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[EmbeddingService#embed] embedding response is empty",
                    EMBEDDING_RESPONSE_EMPTY
            );
        }

        return response.embeddings().getFirst();
    }

    // 배치로 묶어서 전송 (32개씩)
    public List<List<Float>> embedBatch(List<String> texts) {

        List<List<Float>> allEmbeddings = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));

            EmbeddingResponse response = embeddingRestClient.post()
                    .uri("/embed")
                    .body(new EmbeddingRequest(batch))
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || response.embeddings() == null) {
                throw new ServiceException(
                        CommonErrorCode.INTERNAL_SERVER_ERROR,
                        "[EmbeddingService#embedBatch] embedding response is empty",
                        EMBEDDING_RESPONSE_EMPTY
                );
            }

            // 배치 크기와 반환된 임베딩 크기가 일치하는지 확인
            if (response.embeddings().size() != batch.size()) {
                throw new ServiceException(
                        CommonErrorCode.INTERNAL_SERVER_ERROR,
                        "[EmbeddingService#embedBatch] embedding response size mismatch",
                        EMBEDDING_SIZE_MISMATCH
                );
            }

            allEmbeddings.addAll(response.embeddings());
        }

        return allEmbeddings;
    }
}
