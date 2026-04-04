package back.domain.prompt.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.Map;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Jackson DTO 필드는 역직렬화된 값을 그대로 전송용으로 노출한다."
)
public record RepositoryDto(
        @JsonProperty("github_id")
        Long githubId,

        @JsonProperty("name")
        String name,

        @JsonProperty("source_repo")
        String sourceRepo,

        @JsonProperty("source_url")
        String sourceUrl,

        @JsonProperty("summary")
        String summary,

        @JsonProperty("star_count")
        Integer starCount,

        @JsonProperty("fork_count")
        Integer forkCount,

        @JsonProperty("size")
        Integer size,

        @JsonProperty("language_stats")
        Map<String, Integer> languageStats,

        @JsonProperty("license")
        String license,

        @JsonProperty("homepage")
        String homepage,

        @JsonProperty("owner_avatar_url")
        String ownerAvatarUrl,

        @JsonProperty("owner_type")
        String ownerType,

        @JsonProperty("is_official")
        Boolean isOfficial,

        @JsonProperty("default_branch")
        String defaultBranch,

        @JsonProperty("etag")
        String etag,

        @JsonProperty("source_updated_at")
        LocalDateTime sourceUpdatedAt,

        @JsonProperty("active")
        Boolean active,

        @JsonProperty("raw_metadata")
        Map<String, Object> rawMetadata
) {
}
