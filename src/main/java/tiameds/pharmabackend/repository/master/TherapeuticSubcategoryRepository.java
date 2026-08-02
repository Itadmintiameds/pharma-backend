package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.TherapeuticSubcategory;

import java.util.List;

public interface TherapeuticSubcategoryRepository extends JpaRepository<TherapeuticSubcategory, Long> {

    List<TherapeuticSubcategory> findByTherapeuticCategory_TherapeuticCategoryId(Long therapeuticCategoryId);
}
