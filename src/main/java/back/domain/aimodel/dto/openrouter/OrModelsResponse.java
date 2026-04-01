package back.domain.aimodel.dto.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * OpenRouter /api/v1/models raw 응답 구조
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrModelsResponse(
        List<OrModel> data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrModel(
            String id,
            String name,
            String description,

            @JsonProperty("context_length")
            Integer contextLength,

            Architecture architecture,
            Pricing pricing,

            @JsonProperty("top_provider")
            TopProvider topProvider
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Architecture(
                @JsonProperty("input_modalities")
                List<String> inputModalities,

                @JsonProperty("output_modalities")
                List<String> outputModalities
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Pricing(
                String prompt,
                String completion
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record TopProvider(
                @JsonProperty("max_completion_tokens")
                Integer maxCompletionTokens
        ) {}
    }
}
