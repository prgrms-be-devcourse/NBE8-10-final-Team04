package back.domain.info.repository;

import back.domain.info.entity.AiModelFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiModelFamilyRepository extends JpaRepository<AiModelFamily, Integer> {

    // 특정 벤더의 모델 패밀리 전체 조회
    List<AiModelFamily> findByVendorId(Long vendorId);

    // 패밀리명으로 조회
    Optional<AiModelFamily> findByFamilyName(String familyName);

    // 패밀리명 부분 검색 (대소문자 무시)
    List<AiModelFamily> findByFamilyNameContainingIgnoreCase(String keyword);

    // 특정 벤더의 패밀리명으로 조회
    Optional<AiModelFamily> findByVendorIdAndFamilyName(Long vendorId, String familyName);

    // 모델 목록을 함께 fetch join으로 조회
    @Query("SELECT f FROM AiModelFamily f LEFT JOIN FETCH f.models WHERE f.id = :id")
    Optional<AiModelFamily> findByIdWithModels(Integer id);

    // 벤더 정보까지 함께 fetch join으로 조회
    @Query("SELECT f FROM AiModelFamily f JOIN FETCH f.vendor WHERE f.id = :id")
    Optional<AiModelFamily> findByIdWithVendor(Integer id);
}
