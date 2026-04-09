package back.domain.chatbot.service;

import back.domain.chatbot.dto.request.ChatbotRequest;
import back.domain.chatbot.dto.response.ChatbotResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@SuppressFBWarnings("EI_EXPOSE_REP2")
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final WebClient webClient;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public ChatbotResponse chat(String question) {
        return webClient.post()
                .uri(aiServiceUrl + "/chat")
                .bodyValue(new ChatbotRequest(question))
                .retrieve()
                .bodyToMono(ChatbotResponse.class)
                .block();
    }
    public ChatbotResponse getWelcome() {
        return webClient.get()
                .uri(aiServiceUrl + "/chat/welcome")
                .retrieve()
                .bodyToMono(ChatbotResponse.class)
                .block();
    }
}