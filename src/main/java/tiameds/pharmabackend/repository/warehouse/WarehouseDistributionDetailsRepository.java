package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistributionDetails;

import java.util.List;

@Repository
public interface WarehouseDistributionDetailsRepository
        extends JpaRepository<WarehouseDistributionDetails, Long> {

    // all allocation lines for one distribution (iterated during dispatch/receive)
    List<WarehouseDistributionDetails> findByWarehouseDistribution_WarehouseDistributionId(
            Long warehouseDistributionId
    );
}
