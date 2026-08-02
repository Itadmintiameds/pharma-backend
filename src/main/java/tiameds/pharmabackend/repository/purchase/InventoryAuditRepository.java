package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.InventoryAudit;

@Repository
public interface InventoryAuditRepository extends JpaRepository <InventoryAudit, Long> {
}
