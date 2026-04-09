package back.domain.prompt.demo.repository;

import back.domain.prompt.demo.entity.DemoSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DemoSkillRepository extends JpaRepository<DemoSkill, Long> {

}
