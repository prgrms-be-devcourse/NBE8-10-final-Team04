package back.domain.prompt.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Map;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Jackson DTO 필드는 역직렬화된 값을 그대로 전송용으로 노출한다."
)
public record AgentDto(
        @JsonProperty("name")
        String name,

        @JsonProperty("file_path")
        String filePath,

        @JsonProperty("content_md")
        String contentMd,

        @JsonProperty("content_hash")
        String contentHash,

        @JsonProperty("raw_metadata")
        Map<String, Object> rawMetadata
) {
}
