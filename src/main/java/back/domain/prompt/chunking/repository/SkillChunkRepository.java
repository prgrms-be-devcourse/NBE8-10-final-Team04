package back.domain.prompt.chunking.repository;

import back.domain.prompt.chunking.entity.SkillChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkillChunkRepository extends JpaRepository<SkillChunk, Long> {

    List<SkillChunk> findBySkillId(Long skillId);

}