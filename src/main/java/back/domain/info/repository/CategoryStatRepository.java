package back.domain.info.repository;

import back.domain.info.entity.CategoryStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryStatRepository extends JpaRepository<CategoryStat, Long> {
    Optional<CategoryStat> findByCategory(String category);
}
