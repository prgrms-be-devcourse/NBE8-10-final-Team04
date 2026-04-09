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

    public List<ChatbotCardResponse> getCards() {
        return cards == null ? null : List.copyOf(cards);
    }

    public List<String> getNextActions() {
        return nextActions == null ? null : List.copyOf(nextActions);
    }
}
