package back.domain.communitypost.service;

import org.springframework.data.domain.Pageable;

import back.domain.communitypost.dto.AdminChangeCommunityPostStatusRequest;
import back.domain.communitypost.dto.AdminGenerateCommunityPostRequest;
import back.domain.communitypost.dto.AdminUpdateCommunityPostRequest;
import back.domain.communitypost.dto.CommunityPostInfoResponse;
import back.domain.communitypost.dto.PageCommunityPostResponse;
import back.domain.communitypost.entity.CommunityPostStatus;

public interface CommunityPostService extends CommunityPostReadService {

    CommunityPostInfoResponse generatePost(long adminId, AdminGenerateCommunityPostRequest request);

    CommunityPostInfoResponse updatePost(long adminId, long postId, AdminUpdateCommunityPostRequest request);

    void deletePost(long adminId, long postId);

    CommunityPostInfoResponse changeStatus(long adminId, long postId, AdminChangeCommunityPostStatusRequest request);

    PageCommunityPostResponse getPublicPosts(Pageable pageable);

    CommunityPostInfoResponse getPublicPost(long postId);

    PageCommunityPostResponse getAdminPosts(CommunityPostStatus status, Pageable pageable);
}
