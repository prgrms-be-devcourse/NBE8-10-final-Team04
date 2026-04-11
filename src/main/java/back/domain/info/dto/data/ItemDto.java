package back.domain.info.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record ItemDto(
        @JsonProperty("source_id")
        String sourceId,

        @JsonProperty("provider")
        String provider,

        @JsonProperty("family_name")
        String family,

        @JsonProperty("source_type")
        String sourceType,

        @JsonProperty("source_url")
        String soucreUrl,

        @JsonProperty("title")
        String title,

        @JsonProperty("summary")
        String summary,

        @JsonProperty("raw_content")
        String rawContent,

        @JsonProperty("notified_at")
        LocalDateTime notifiedAt
) {
}
