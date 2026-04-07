package back.domain.info.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.OffsetDateTime;
import java.util.List;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Jackson DTO 필드는 역직렬화된 값을 그대로 전송용으로 노출한다."
)
public record UpdateRequestDto(
        @JsonProperty("collected_at")
        OffsetDateTime collectedAt,

        @JsonProperty("count")
        Long count,

        @JsonProperty("items")
        List<ItemDto> items
) {
}
