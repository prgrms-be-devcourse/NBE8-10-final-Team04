package back.domain.info.mapper;

import back.domain.info.dto.data.FamilyDto;
import back.domain.info.dto.data.VendorDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelMapperTest {

    private final AiModelMapper mapper = new AiModelMapper();

    @Test
    void toVendorEntity_mapsVendorAndFamilies() {
        FamilyDto familyDto = new FamilyDto(
                "GPT-4.1",
                "Flagship",
                new String[]{"text"},
                new String[]{"text", "image"}
        );
        VendorDto vendorDto = new VendorDto(
                "OpenAI",
                "https://openai.com",
                true,
                false,
                List.of(familyDto)
        );

        var vendor = mapper.toVendorEntity(vendorDto);

        assertThat(vendor.getName()).isEqualTo("OpenAI");
        assertThat(vendor.getOfficialUrl()).isEqualTo("https://openai.com");
        assertThat(vendor.getIsActive()).isTrue();
        assertThat(vendor.getIsDeprecated()).isFalse();
        assertThat(vendor.getModelFamilies()).hasSize(1);
        assertThat(vendor.getModelFamilies().getFirst().getVendor()).isSameAs(vendor);
        assertThat(vendor.getModelFamilies().getFirst().getFamilyName()).isEqualTo("GPT-4.1");
        assertThat(vendor.getModelFamilies().getFirst().getCommonDescription()).isEqualTo("Flagship");
        assertThat(vendor.getModelFamilies().getFirst().getInputTypes()).containsExactly("text");
        assertThat(vendor.getModelFamilies().getFirst().getOutputTypes()).containsExactly("text", "image");
    }

    @Test
    void toVendorEntity_handlesNullFamilies() {
        VendorDto vendorDto = new VendorDto(
                "Anthropic",
                "https://anthropic.com",
                true,
                false,
                null
        );

        var vendor = mapper.toVendorEntity(vendorDto);

        assertThat(vendor.getModelFamilies()).isEmpty();
    }

    @Test
    void toFamilyEntity_mapsFields() {
        FamilyDto familyDto = new FamilyDto(
                "Claude 4",
                "Reasoning family",
                new String[]{"text", "image"},
                new String[]{"text"}
        );

        var vendor = back.domain.info.entity.AiVendor.builder()
                .name("Anthropic")
                .officialUrl("https://anthropic.com")
                .isActive(true)
                .isDeprecated(false)
                .build();

        var family = mapper.toFamilyEntity(familyDto, vendor);

        assertThat(family.getVendor()).isSameAs(vendor);
        assertThat(family.getFamilyName()).isEqualTo("Claude 4");
        assertThat(family.getCommonDescription()).isEqualTo("Reasoning family");
        assertThat(family.getInputTypes()).containsExactly("text", "image");
        assertThat(family.getOutputTypes()).containsExactly("text");
    }
}
