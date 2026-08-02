package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.AgeGroup;

public interface AgeGroupRepository extends JpaRepository<AgeGroup, Long> {
}
