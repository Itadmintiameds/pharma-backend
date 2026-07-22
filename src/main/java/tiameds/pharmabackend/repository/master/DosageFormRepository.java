package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.DosageForm;

public interface DosageFormRepository extends JpaRepository<DosageForm, Long> {
}
