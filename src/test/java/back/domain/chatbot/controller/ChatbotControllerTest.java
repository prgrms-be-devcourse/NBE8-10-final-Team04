package back.domain.chatbot.controller;

import back.domain.chatbot.dto.request.ChatbotRequest;
import back.domain.chatbot.dto.response.ChatbotResponse;
import back.domain.chatbot.service.ChatbotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatbotController.class)
class ChatbotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatbotService chatbotService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser // Spring Security가 설정되어 있다면 권한 처리를 위해 필요합니다.
    @DisplayName("챗봇에게 질문을 던지면 200 OK와 응답 메시지를 반환한다")
    void chat_Success() throws Exception {
        // given
        ChatbotRequest request = new ChatbotRequest("오늘 날씨 어때?");

        // ChatbotResponse는 NoArgsConstructor가 있으므로 Mockito가 가짜 객체를 만들기 쉽습니다.
        // 실제 서비스 로직은 타지 않고 가짜 응답을 내보내도록 설정합니다.
        given(chatbotService.chat(anyString())).willReturn(new ChatbotResponse());

        // when & then
        mockMvc.perform(post("/api/chatbot")
                        .with(csrf()) // Spring Security 사용 시 POST 요청에 CSRF 토큰이 필요할 수 있습니다.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("인사말 GET 요청 시 200 OK와 초기 메시지를 반환한다")
    void getWelcome_ApiSuccess() throws Exception {
        // given
        ChatbotResponse mockResponse = new ChatbotResponse();
        // 가짜 객체에 필요한 값 설정 (Reflection 혹은 실제 필드 구조에 따라)
        given(chatbotService.getWelcome()).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/chatbot/welcome"))
                .andExpect(status().isOk());
    }
}