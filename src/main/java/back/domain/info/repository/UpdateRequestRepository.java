package back.domain.info.repository;

import back.domain.info.entity.UpdateRequest;
import back.domain.info.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface UpdateRequestRepository extends JpaRepository<UpdateRequest, Long> {
    Page<UpdateRequest> findAllByStatus(Status status, Pageable pageable);

    java.util.List<UpdateRequest> findAllByStatusAndVendorIdAndNotifiedAtOrderByReviewedAtDescCreatedAtDesc(
            Status status, Long vendorId, LocalDate notifiedAt);

    boolean existsBySourceIdAndNotifiedAt(String sourceId, LocalDate notifiedAt);
}
