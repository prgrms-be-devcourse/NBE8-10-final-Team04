package back.domain.communitypost.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "응답 DTO 직렬화를 위해 페이지 내용을 그대로 노출한다.")
public record PageCommunityPostResponse(
        List<CommunityPostInfoResponse> contents, long totalElements, int totalPages, int page, int size) {

    public static PageCommunityPostResponse from(Page<CommunityPostInfoResponse> page) {
        return new PageCommunityPostResponse(
                page.getContent(), page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }
}
