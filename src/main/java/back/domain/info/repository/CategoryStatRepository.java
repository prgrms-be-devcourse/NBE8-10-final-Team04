package back.domain.info.repository;

import back.domain.info.entity.CategoryStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryStatRepository extends JpaRepository<CategoryStat, Long> {
}
