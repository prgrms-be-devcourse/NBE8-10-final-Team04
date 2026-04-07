package back.domain.prompt.prompt.repository;

import back.domain.prompt.prompt.entity.Skill;
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

    @Query("SELECT s FROM Skill s JOIN FETCH s.repository WHERE s.id = :id")
    Optional<Skill> findByIdWithRepository(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Skill s SET s.isChunked = true WHERE s.id = :id")
    void markAsChunked(@Param("id") Long id);
}
