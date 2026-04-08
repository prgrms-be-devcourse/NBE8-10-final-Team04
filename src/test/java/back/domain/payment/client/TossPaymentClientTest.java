package back.domain.payment.client;

import back.domain.payment.dto.request.PaymentConfirmRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
class TossPaymentClientTest {

    @Autowired
    private TossPaymentClient tossPaymentClient;

    @Test
    @DisplayName("토스 승인 API 호출 시 에러 없이 동작해야 한다")
    void confirm_api_call() {
        // 1. 클라이언트 내부의 RestTemplate을 가져오거나 새로 설정합니다.
        RestTemplate restTemplate = new RestTemplate();
        // ReflectionTestUtils를 사용해 클라이언트 내부의 restTemplate 필드에 주입 (필요시)
        ReflectionTestUtils.setField(tossPaymentClient, "restTemplate", restTemplate);

        // 2. 가짜 서버(MockRestServiceServer)를 수동으로 생성합니다.
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        // 3. 서버 기대 설정
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(withSuccess("{\"status\":\"DONE\"}", MediaType.APPLICATION_JSON));

        // 4. 실행
        PaymentConfirmRequest request = new PaymentConfirmRequest("key-1", "order-1", 990L );

        assertDoesNotThrow(() -> tossPaymentClient.confirm(request));
    }
}