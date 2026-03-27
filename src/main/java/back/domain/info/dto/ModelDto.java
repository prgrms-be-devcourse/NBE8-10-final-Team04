package back.domain.info.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class ModelDto {

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("api_id")
    private String apiId;

    @JsonProperty("context_window")
    private Integer contextWindow;

    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("is_preview")
    private Boolean isPreview;

    @JsonProperty("model_image_url")
    private String modelImageUrl;

    @JsonProperty("input_price")
    private BigDecimal inputPrice;

    @JsonProperty("output_price")
    private BigDecimal outputPrice;

    @JsonProperty("input_modalities")
    private List<String> inputModalities;

    @JsonProperty("output_modalities")
    private List<String> outputModalities;

//    private String category;
}
