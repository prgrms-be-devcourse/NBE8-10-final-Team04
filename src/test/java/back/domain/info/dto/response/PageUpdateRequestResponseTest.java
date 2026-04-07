package back.domain.info.dto.response;

import back.domain.info.enums.Status;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageUpdateRequestResponseTest {

    @Test
    void constructor_mapsPageMetadataAndContents() {
        UpdateRequestResponse item = new UpdateRequestResponse(
                1L,
                "item-1",
                "OpenAI",
                "GPT",
                "https://example.com/post",
                "blog",
                "summary",
                Status.APPROVED,
                LocalDate.of(2026, 4, 3),
                LocalDateTime.of(2026, 4, 3, 10, 0),
                LocalDateTime.of(2026, 4, 3, 9, 0)
        );

        PageUpdateRequestResponse response = new PageUpdateRequestResponse(
                new PageImpl<>(List.of(item), PageRequest.of(2, 5), 11)
        );

        assertThat(response.contents()).containsExactly(item);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(5);
    }
}
