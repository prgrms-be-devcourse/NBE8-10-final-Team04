package back.domain.communitypost.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateCommunityPostRequest(
        @NotBlank(message = "title-NotBlank-제목은 필수입니다.") @Size(max = 255, message = "title-Size-제목은 255자 이하여야 합니다.")
                String title,
        String summary,
        String sourceUrl) {}
