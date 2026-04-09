package back.domain.chatbot.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatbotTopPickResponse {
    private Long id;
    private String title;
    private String description;
    private String reason;
    private Double score;
    private String howToUse;
}