package back.domain.info.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import back.domain.info.entity.AiModelFamily;

@Repository
public interface AiModelFamilyRepository extends JpaRepository<AiModelFamily, Long> {
    Optional<AiModelFamily> findByFamilyName(String familyName);

    List<AiModelFamily> findAllByVendorIdOrderByFamilyNameAsc(Long vendorId);

    @Query(
            """
            select f
            from AiModelFamily f
            join fetch f.vendor v
            where f.id = :familyId
            """)
    Optional<AiModelFamily> findWithVendorById(@Param("familyId") Long familyId);

    @Query(
            """
            select f
            from AiModelFamily f
            join fetch f.vendor v
            where (f.createdAt >= :start and f.createdAt < :end)
               or (f.updatedAt >= :start and f.updatedAt < :end)
            order by v.name asc, f.familyName asc
            """)
    List<AiModelFamily> findAllChangedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(
            """
            select f
            from AiModelFamily f
            join fetch f.vendor v
            where v.id = :vendorId
              and ((f.createdAt >= :start and f.createdAt < :end)
                or (f.updatedAt >= :start and f.updatedAt < :end))
            order by f.familyName asc
            """)
    List<AiModelFamily> findAllChangedBetweenAndVendorId(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("vendorId") Long vendorId);
}
