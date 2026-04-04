package back.domain.info.mapper;

import back.domain.info.dto.data.ItemDto;
import back.domain.info.entity.AiModelFamily;
import back.domain.info.entity.AiVendor;
import back.domain.info.enums.Status;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RequestMapperTest {

    private final RequestMapper mapper = new RequestMapper();

    @Test
    void toUpdateRequestEntity_mapsFields() {
        ItemDto dto = new ItemDto(
                "item-1",
                "OpenAI",
                "GPT",
                "blog",
                null,
                null,
                "https://example.com/post",
                "summary",
                "raw content",
                null
        );

        AiVendor vendor = AiVendor.builder()
                .name("OpenAI")
                .officialUrl("https://openai.com")
                .isActive(true)
                .isDeprecated(false)
                .build();
        AiModelFamily family = AiModelFamily.builder()
                .vendor(vendor)
                .familyName("GPT")
                .commonDescription("family")
                .build();

        var entity = mapper.toUpdateRequestEntity(dto, vendor, family);

        assertThat(entity.getSourceId()).isEqualTo("item-1");
        assertThat(entity.getVendor()).isSameAs(vendor);
        assertThat(entity.getFamily()).isSameAs(family);
        assertThat(entity.getSourceUrl()).isEqualTo("https://example.com/post");
        assertThat(entity.getSourceType()).isEqualTo("blog");
        assertThat(entity.getRawContent()).isEqualTo("raw content");
        assertThat(entity.getSummary()).isEqualTo("summary");
        assertThat(entity.getStatus()).isEqualTo(Status.PENDING);
        assertThat(entity.getNotifiedAt()).isEqualTo(LocalDate.now());
    }
}
