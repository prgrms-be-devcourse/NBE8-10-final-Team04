package back.domain.chatbot.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChatbotResponse {
    private String question;
    private String questionType;
    private String targetType;
    private String message;
    private List<ChatbotCardResponse> cards;
    private ChatbotTopPickResponse topPick;
    private List<String> nextActions;
    private boolean outOfScope;
}
