package back.domain.aimodel.dto.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenRouter /api/v1/models raw 응답 구조
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrModelsResponse(
        List<OrModel> data
) {
    public OrModelsResponse(List<OrModel> data) {
        this.data = data == null ? List.of() : List.copyOf(data);
    }

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
        ) {
            public Architecture(List<String> inputModalities, List<String> outputModalities) {
                this.inputModalities  = inputModalities  == null ? List.of() : List.copyOf(inputModalities);
                this.outputModalities = outputModalities == null ? List.of() : List.copyOf(outputModalities);
            }
        }

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