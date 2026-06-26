package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.PharmaRoles;

@Repository
public interface PharmaRolesRepository extends JpaRepository<PharmaRoles, Long> {
}
