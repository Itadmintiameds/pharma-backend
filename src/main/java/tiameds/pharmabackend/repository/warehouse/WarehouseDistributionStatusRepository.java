package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistributionStatus;
import tiameds.pharmabackend.enums.DistributionStatus;
import tiameds.pharmabackend.enums.LocationType;

import java.time.LocalDateTime;
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

    // KPI: how many distributions shipped TO this destination currently sit at
    // :status (their latest status row equals :status). Used for "pending receipts"
    // with :status = PRODUCTS_DISPATCHED. The max-id subquery mirrors the latest-status
    // semantics of findLatestStatuses so a later STOCK_RECEIVED supersedes the dispatch.
    @Query("""
        SELECT COUNT(s) FROM WarehouseDistributionStatus s
        WHERE s.warehouseDistribution.destinationType = :destinationType
          AND s.warehouseDistribution.destinationId = :destinationId
          AND s.warehouseDistributionStatus = :status
          AND s.warehouseDistributionStatusId = (
              SELECT MAX(s2.warehouseDistributionStatusId)
              FROM WarehouseDistributionStatus s2
              WHERE s2.warehouseDistribution = s.warehouseDistribution
          )
    """)
    long countByDestinationAndLatestStatus(
            @Param("destinationType") LocationType destinationType,
            @Param("destinationId") String destinationId,
            @Param("status") DistributionStatus status);

    // KPI: how many distributions shipped FROM this source currently sit at :status
    // (their latest status row equals :status). Used for the source screen's
    // readyToDispatch (DISTRIBUTION_CREATED), pendingReceipt (PRODUCTS_DISPATCHED) and
    // completed (STOCK_RECEIVED). The max-id subquery mirrors the latest-status
    // semantics of findLatestStatuses.
    @Query("""
        SELECT COUNT(s) FROM WarehouseDistributionStatus s
        WHERE s.warehouseDistribution.sourceType = :sourceType
          AND s.warehouseDistribution.sourceId = :sourceId
          AND s.warehouseDistributionStatus = :status
          AND s.warehouseDistributionStatusId = (
              SELECT MAX(s2.warehouseDistributionStatusId)
              FROM WarehouseDistributionStatus s2
              WHERE s2.warehouseDistribution = s.warehouseDistribution
          )
    """)
    long countBySourceAndLatestStatus(
            @Param("sourceType") LocationType sourceType,
            @Param("sourceId") String sourceId,
            @Param("status") DistributionStatus status);

    // KPI: how many distributions shipped TO this destination transitioned to :status
    // within [start, end) — used for "received today" with :status = STOCK_RECEIVED.
    // A distribution reaches STOCK_RECEIVED exactly once (the receive guard forbids a
    // second receipt), so counting the status rows in the window counts the receipts.
    @Query("""
        SELECT COUNT(s) FROM WarehouseDistributionStatus s
        WHERE s.warehouseDistribution.destinationType = :destinationType
          AND s.warehouseDistribution.destinationId = :destinationId
          AND s.warehouseDistributionStatus = :status
          AND s.createdAt >= :start AND s.createdAt < :end
    """)
    long countByDestinationAndStatusInPeriod(
            @Param("destinationType") LocationType destinationType,
            @Param("destinationId") String destinationId,
            @Param("status") DistributionStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
