package back.domain.info.dto.response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Page;

import java.util.List;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "응답 DTO로 직렬화를 위해 페이지 내용을 그대로 노출한다."
)
public record PageUpdateRequestResponse(
        List<UpdateRequestResponse> contents,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public PageUpdateRequestResponse(Page<UpdateRequestResponse> page) {
        this(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
