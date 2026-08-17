package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistributionStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseDistributionStatusRepository
        extends JpaRepository<WarehouseDistributionStatus, Long> {

    // status history for one distribution, oldest first
    List<WarehouseDistributionStatus> findByWarehouseDistribution_WarehouseDistributionIdOrderByCreatedAtAsc(
            Long warehouseDistributionId
    );

    // current (latest) status — ordered by PK so equal timestamps still resolve deterministically
    Optional<WarehouseDistributionStatus>
    findFirstByWarehouseDistribution_WarehouseDistributionIdOrderByWarehouseDistributionStatusIdDesc(
            Long warehouseDistributionId
    );
}
