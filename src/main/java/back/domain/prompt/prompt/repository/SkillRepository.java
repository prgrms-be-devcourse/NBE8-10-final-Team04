package back.domain.prompt.prompt.repository;

import back.domain.prompt.prompt.entity.Skill;
import back.domain.prompt.prompt.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByRepositoryIdAndName(Long repositoryId, String name);

    List<Skill> findByRepositoryId(Long repositoryId);

    List<Skill> findAllByOrderByIdAsc();

    List<Skill> findByIsChunkedFalse();

    @Query("SELECT s.id FROM Skill s WHERE s.isChunked = false ORDER BY s.id ASC")
    List<Long> findIdsByIsChunkedFalse();

    @Query("SELECT s FROM Skill s JOIN FETCH s.repository WHERE s.id = :id")
    Optional<Skill> findByIdWithRepository(@Param("id") Long id);

    @Query("SELECT s FROM Skill s JOIN FETCH s.repository WHERE s.id IN :ids")
    List<Skill> findAllByIdsWithRepository(@Param("ids") List<Long> ids);

    @Query(value = "SELECT s FROM Skill s JOIN FETCH s.repository",
           countQuery = "SELECT COUNT(s) FROM Skill s")
    Page<Skill> findAllWithRepository(Pageable pageable);

    @Query(value = """
                   SELECT s.id FROM skills s
                   WHERE s.name ILIKE :keyword
                      OR s.tags_json::text ILIKE :keyword
                      OR EXISTS (
                          SELECT 1 FROM repositories r
                          WHERE r.id = s.repository_id
                            AND (r.name ILIKE :keyword OR r.summary ILIKE :keyword)
                      )
                   ORDER BY s.id
                   """,
           nativeQuery = true)
    List<Long> searchIdsByKeyword(@Param("keyword") String keyword);

    @Query(value = "SELECT s FROM Skill s JOIN FETCH s.repository WHERE s.category = :category",
           countQuery = "SELECT COUNT(s) FROM Skill s WHERE s.category = :category")
    Page<Skill> findByCategoryWithRepository(@Param("category") Category category, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Skill s SET s.isChunked = true WHERE s.id = :id")
    void markAsChunked(@Param("id") Long id);
}
