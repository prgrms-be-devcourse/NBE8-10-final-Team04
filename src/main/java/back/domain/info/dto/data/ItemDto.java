package back.domain.info.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record ItemDto(
        @JsonProperty("id")
        String itemId,

        @JsonProperty("provider")
        String provider,

        @JsonProperty("family")
        String family,

        @JsonProperty("source_type")
        String sourceType,

        @JsonProperty("label")
        String label,

        @JsonProperty("title")
        String title,

        @JsonProperty("url")
        String url,

        @JsonProperty("summary")
        String summary,

        @JsonProperty("raw_content")
        String rawContent,

        @JsonProperty("published_at")
        OffsetDateTime publishedAt
) {
}
