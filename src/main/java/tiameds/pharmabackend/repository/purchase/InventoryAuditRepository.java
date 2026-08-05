package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.InventoryAudit;

import java.util.List;

@Repository
public interface InventoryAuditRepository extends JpaRepository <InventoryAudit, Long> {

    List<InventoryAudit> findByBilling_BillingId(Long billingId);
}
