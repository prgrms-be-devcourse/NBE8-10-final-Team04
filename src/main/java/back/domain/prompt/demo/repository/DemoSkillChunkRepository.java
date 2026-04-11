package back.domain.prompt.demo.repository;

import back.domain.prompt.demo.entity.DemoSkillChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DemoSkillChunkRepository extends JpaRepository<DemoSkillChunk, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM DemoSkillChunk c WHERE c.demoSkill.id = :demoSkillId")
    void deleteByDemoSkillId(@Param("demoSkillId") Long demoSkillId);
}
