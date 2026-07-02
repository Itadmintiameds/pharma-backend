package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.PharmacyOrganization;

@Repository
public interface PharmacyOrganizationRepository extends JpaRepository <PharmacyOrganization, Long> {
}
