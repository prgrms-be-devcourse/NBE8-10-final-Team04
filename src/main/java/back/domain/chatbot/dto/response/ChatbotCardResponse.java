package back.domain.chatbot.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatbotCardResponse {
    private Long id;
    private String title;
    private String owner;
    private Integer star;
    private String description;
    private String uploadedAt;
    private Integer like;
    private String reason;
    private Double score;
    private String type;
}