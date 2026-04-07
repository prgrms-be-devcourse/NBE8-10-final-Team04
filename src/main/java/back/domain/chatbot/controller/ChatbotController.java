package back.domain.chatbot.controller;

import back.domain.chatbot.dto.request.ChatbotRequest;
import back.domain.chatbot.dto.response.ChatbotResponse;
import back.domain.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping
    public ChatbotResponse chat(@RequestBody ChatbotRequest request) {
        return chatbotService.chat(request.getQuestion());
    }

    @GetMapping("/welcome")
    public ChatbotResponse welcome() {
        return chatbotService.getWelcome();
    }
}