package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistributionStatus;

import java.util.Collection;
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

    // Latest status row (highest PK per distribution) for the distributions on the
    // page, so the list screen doesn't re-query status per row and never scans the
    // whole status history.
    @Query("""
        SELECT s FROM WarehouseDistributionStatus s
        WHERE s.warehouseDistributionStatusId IN (
            SELECT MAX(s2.warehouseDistributionStatusId)
            FROM WarehouseDistributionStatus s2
            WHERE s2.warehouseDistribution.warehouseDistributionId IN :distributionIds
            GROUP BY s2.warehouseDistribution.warehouseDistributionId
        )
    """)
    List<WarehouseDistributionStatus> findLatestStatuses(
            @Param("distributionIds") Collection<Long> distributionIds);
}
