package back.domain.chatbot.service;

import back.domain.chatbot.dto.response.ChatbotResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotServiceTest {

    private static MockWebServer mockWebServer;
    private ChatbotService chatbotService;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void initialize() {
        // MockWebServer의 URL을 사용하는 WebClient 생성
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        WebClient webClient = WebClient.create();

        chatbotService = new ChatbotService(webClient);

        // @Value 필드 강제 주입 (ReflectionTestUtils 사용)
        ReflectionTestUtils.setField(chatbotService, "aiServiceUrl", baseUrl);
    }

    @Test
    @DisplayName("외부 AI 서비스에 질문을 보내고 상세 응답을 정상적으로 받는다")
    void chat_Success() {
        // given (JSON 응답을 DTO 구조와 똑같이 맞춰줍니다)
        String mockJsonResponse = """
        {
            "question": "테스트 질문",
            "message": "안녕하세요, AI 응답입니다.",
            "outOfScope": false,
            "cards": [],
            "nextActions": ["다음 단계1", "다음 단계2"]
        }
        """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockJsonResponse)
                .addHeader("Content-Type", "application/json"));

        // when
        ChatbotResponse response = chatbotService.chat("테스트 질문");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getQuestion()).isEqualTo("테스트 질문");
        assertThat(response.getMessage()).isEqualTo("안녕하세요, AI 응답입니다."); // getAnswer 대신 getMessage 사용
        assertThat(response.isOutOfScope()).isFalse();
        assertThat(response.getNextActions()).hasSize(2);
    }

    @Test
    @DisplayName("인사말 API 호출 시 FastAPI로부터 받은 초기 메시지를 반환한다")
    void getWelcome_Success() {
        // given
        String mockWelcomeResponse = """
        {
            "question": "INIT",
            "message": "안녕하세요! AI 툴 추천, 스킬 추천을 도와드립니다!",
            "nextActions": ["AI 툴 추천해줘", "내게 맞는 스킬은?"],
            "outOfScope": false
        }
        """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockWelcomeResponse)
                .addHeader("Content-Type", "application/json"));

        // when
        ChatbotResponse response = chatbotService.getWelcome();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("안녕하세요");
        assertThat(response.getNextActions()).hasSize(2);
        assertThat(response.getNextActions()).contains("AI 툴 추천해줘");
    }
}