package back.domain.communitypost.service;

import back.domain.communitypost.entity.CommunityPost;

public interface CommunityPostReadService {

    CommunityPost getPublishedPostOrThrow(long postId);

    CommunityPost getPostOrThrow(long postId);
}
