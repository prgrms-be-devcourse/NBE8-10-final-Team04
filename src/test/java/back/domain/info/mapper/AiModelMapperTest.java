package back.domain.info.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.info.dto.FamilyDto;
import back.domain.info.dto.ModelDto;
import back.domain.info.dto.VendorDto;

class AiModelMapperTest {

    private final AiModelMapper mapper = new AiModelMapper();

    @Test
    @DisplayName("벤더 DTO를 벤더-패밀리-모델 엔티티 계층으로 변환한다")
    void toVendorEntity_mapsHierarchy() {
        ModelDto modelDto = new ModelDto();
        ReflectionTestUtils.setField(modelDto, "modelName", "GPT-4.1");
        ReflectionTestUtils.setField(modelDto, "apiId", "gpt-4.1");
        ReflectionTestUtils.setField(modelDto, "contextWindow", 128000);
        ReflectionTestUtils.setField(modelDto, "maxOutputTokens", 4096);
        ReflectionTestUtils.setField(modelDto, "releaseDate", LocalDate.of(2025, 1, 10));
        ReflectionTestUtils.setField(modelDto, "isPreview", false);
        ReflectionTestUtils.setField(modelDto, "modelImageUrl", "https://example.com/gpt-4.1.png");
        ReflectionTestUtils.setField(modelDto, "inputPrice", new BigDecimal("2.50"));
        ReflectionTestUtils.setField(modelDto, "outputPrice", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(modelDto, "inputModalities", List.of("text", "image"));
        ReflectionTestUtils.setField(modelDto, "outputModalities", List.of("text"));

        FamilyDto familyDto = new FamilyDto();
        ReflectionTestUtils.setField(familyDto, "familyName", "GPT-4.1");
        ReflectionTestUtils.setField(familyDto, "commonDescription", "Flagship family");
        ReflectionTestUtils.setField(familyDto, "models", List.of(modelDto));

        VendorDto vendorDto = new VendorDto();
        ReflectionTestUtils.setField(vendorDto, "name", "OpenAI");
        ReflectionTestUtils.setField(vendorDto, "officialUrl", "https://openai.com");
        ReflectionTestUtils.setField(vendorDto, "isActive", true);
        ReflectionTestUtils.setField(vendorDto, "isDeprecated", false);
        ReflectionTestUtils.setField(vendorDto, "families", List.of(familyDto));

        var vendor = mapper.toVendorEntity(vendorDto);

        assertThat(vendor.getName()).isEqualTo("OpenAI");
        assertThat(vendor.getModelFamilies()).hasSize(1);
        assertThat(vendor.getModelFamilies().getFirst().getVendor()).isSameAs(vendor);
        assertThat(vendor.getModelFamilies().getFirst().getModels()).hasSize(1);
        assertThat(vendor.getModelFamilies().getFirst().getModels().getFirst().getApiId()).isEqualTo("gpt-4.1");
        assertThat(vendor.getModelFamilies().getFirst().getModels().getFirst().getFamily())
                .isSameAs(vendor.getModelFamilies().getFirst());
    }

    @Test
    @DisplayName("모달리티가 null이면 빈 리스트로 변환한다")
    void toModelEntity_handlesNullModalities() {
        ModelDto modelDto = new ModelDto();
        ReflectionTestUtils.setField(modelDto, "modelName", "Claude Sonnet");
        ReflectionTestUtils.setField(modelDto, "apiId", "claude-sonnet");
        ReflectionTestUtils.setField(modelDto, "releaseDate", LocalDate.of(2025, 2, 1));
        ReflectionTestUtils.setField(modelDto, "inputModalities", null);
        ReflectionTestUtils.setField(modelDto, "outputModalities", null);

        var family = back.domain.info.entity.AiModelFamily.builder()
                .familyName("Claude")
                .models(new java.util.ArrayList<>())
                .build();

        var model = mapper.toModelEntity(modelDto, family);

        assertThat(model.getInputModalities()).isEmpty();
        assertThat(model.getOutputModalities()).isEmpty();
    }
}
