package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.WarehouseInventoryAudit;

import java.util.List;

@Repository
public interface WarehouseInventoryAuditRepository extends JpaRepository<WarehouseInventoryAudit, Long> {

    // ledger of movements caused by one allocation line (traceability)
    List<WarehouseInventoryAudit> findByWarehouseDistributionDetails_WarehouseDistributionDetailsId(
            Long warehouseDistributionDetailsId
    );
}
