package back.domain.info.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class UpdateRequestDto {
    @JsonProperty("collected_at")
    private OffsetDateTime collectedAt;

    @JsonProperty("count")
    private Long count;

    @JsonProperty("items")
    private List<ItemDto> items;
}
