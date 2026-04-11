package back.domain.communitypost.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import back.domain.communitypost.entity.CommunityPost;
import back.domain.communitypost.entity.CommunityPostStatus;
import back.domain.communitypost.entity.CommunityPostType;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    Page<CommunityPost> findAllByStatus(CommunityPostStatus status, Pageable pageable);

    Optional<CommunityPost> findByIdAndStatus(Long postId, CommunityPostStatus status);

    boolean existsByPostTypeAndTargetDate(CommunityPostType postType, LocalDate targetDate);

    boolean existsByPostTypeAndTargetDateAndVendorId(CommunityPostType postType, LocalDate targetDate, Long vendorId);
}
