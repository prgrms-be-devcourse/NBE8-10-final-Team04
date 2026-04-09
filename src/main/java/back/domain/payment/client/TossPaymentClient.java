package back.domain.payment.client;

import back.domain.payment.dto.request.PaymentConfirmRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Slf4j
public class TossPaymentClient {

    @Value("${toss.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void confirm(PaymentConfirmRequest request) {
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", basicAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PaymentConfirmRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.tosspayments.com/v1/payments/confirm",
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("토스 결제 승인 실패: " + response.getBody());
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("#### 토스 API 에러 상세 정보: {}", e.getResponseBodyAsString());
            throw new RuntimeException("토스 API 오류: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Toss API 통신 중 알 수 없는 오류 발생: {}", e.getMessage());
            throw new RuntimeException("결제 통신 중 오류 발생: " + e.getMessage());
        }
    }
}