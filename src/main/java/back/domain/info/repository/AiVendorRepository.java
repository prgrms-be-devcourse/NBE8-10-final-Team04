package back.domain.info.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import back.domain.info.entity.AiVendor;

@Repository
public interface AiVendorRepository extends JpaRepository<AiVendor, Long> {

    // 이름으로 조회
    Optional<AiVendor> findByName(String name);

    List<AiVendor> findAllByOrderByNameAsc();
}
