package back.domain.info.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageUpdateRequestResponse {
    private final List<UpdateRequestResponse> contents;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;

    public PageUpdateRequestResponse(Page<UpdateRequestResponse> page) {
        this.contents = page.getContent();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.page = page.getNumber();
        this.size = page.getSize();
    }
}
