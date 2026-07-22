package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.TherapeuticCategory;

public interface TherapeuticCategoryRepository extends JpaRepository<TherapeuticCategory, Long> {
}
