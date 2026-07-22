package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.Flavour;

public interface FlavourRepository extends JpaRepository<Flavour, Long> {

    boolean existsByFlavourNameIgnoreCase(String flavourName);
}
