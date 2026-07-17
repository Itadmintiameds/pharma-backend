package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.PharmaPermission;

@Repository
public interface PharmaPermissionRepository extends JpaRepository<PharmaPermission, Long> {
}
