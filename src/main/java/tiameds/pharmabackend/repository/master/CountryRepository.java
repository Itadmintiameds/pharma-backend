package tiameds.pharmabackend.repository.master;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.master.Country;

public interface CountryRepository extends JpaRepository<Country, Long> {
}
