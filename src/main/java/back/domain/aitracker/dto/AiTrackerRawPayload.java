package back.domain.aitracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * OCI data/ai-tracker/updates_raw.json 파싱용 페이로드.
 *
 * <p>Python collector 가 생성한 원본 구조와 동일합니다.
 */
public record AiTrackerRawPayload(
        @JsonProperty("collected_at") Instant collectedAt,
        int count,
        List<RawItem> items) {

    /** 개별 수집 항목. */
    public record RawItem(
            String id,
            String provider,
            @JsonProperty("source_type") String sourceType,
            String label,
            String title,
            String url,
            String summary,
            @JsonProperty("raw_content") String rawContent,
            @JsonProperty("published_at") Instant publishedAt) {}
}
