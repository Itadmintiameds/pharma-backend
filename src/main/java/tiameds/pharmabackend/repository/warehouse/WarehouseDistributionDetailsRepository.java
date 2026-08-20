package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistributionDetails;
import tiameds.pharmabackend.enums.DistributionStatus;
import tiameds.pharmabackend.enums.LocationType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface WarehouseDistributionDetailsRepository
        extends JpaRepository<WarehouseDistributionDetails, Long> {

    // all allocation lines for one distribution (iterated during dispatch/receive)
    List<WarehouseDistributionDetails> findByWarehouseDistribution_WarehouseDistributionId(
            Long warehouseDistributionId
    );

    // Lines for one distribution with product, packaging and batch fetched eagerly, so
    // the detail view (findById) can expose each line's product/packaging/batch without
    // N+1 queries or lazy-initialization issues.
    @Query("""
        SELECT d FROM WarehouseDistributionDetails d
        LEFT JOIN FETCH d.product
        LEFT JOIN FETCH d.packaging
        LEFT JOIN FETCH d.batch
        WHERE d.warehouseDistribution.warehouseDistributionId = :distributionId
    """)
    List<WarehouseDistributionDetails> findLinesWithProductGraph(
            @Param("distributionId") Long distributionId);

    // Per-distribution line totals for the list screen: how many distinct products and
    // the issued / dispatched / received quantity totals. One grouped query for the
    // distributions on the page (scoped by id) avoids an N+1 and never touches
    // unrelated rows. Dispatched/received are COALESCEd since they are null until the
    // dispatch/receive steps run.
    @Query("""
        SELECT d.warehouseDistribution.warehouseDistributionId AS distributionId,
               COUNT(DISTINCT d.product.productId) AS productsCount,
               COALESCE(SUM(d.issueQuantity), 0) AS totalQuantity,
               COALESCE(SUM(d.dispatchedQuantity), 0) AS dispatchedQuantity,
               COALESCE(SUM(d.receivedQuantity), 0) AS receivedQuantity
        FROM WarehouseDistributionDetails d
        WHERE d.warehouseDistribution.warehouseDistributionId IN :distributionIds
        GROUP BY d.warehouseDistribution.warehouseDistributionId
    """)
    List<DistributionLineAggregate> aggregateLinesByDistribution(
            @Param("distributionIds") Collection<Long> distributionIds);

    // KPI: total quantity of products received TO this destination whose distribution
    // transitioned to :status within [start, end) — used for "products received today"
    // with :status = STOCK_RECEIVED. Sums the actual received quantity of every line
    // belonging to a distribution received in the window. receivedQuantity is COALESCEd
    // since it is null on lines that never completed the receive step.
    @Query("""
        SELECT COALESCE(SUM(d.receivedQuantity), 0)
        FROM WarehouseDistributionDetails d
        WHERE d.warehouseDistribution.destinationType = :destinationType
          AND d.warehouseDistribution.destinationId = :destinationId
          AND EXISTS (
              SELECT 1 FROM WarehouseDistributionStatus s
              WHERE s.warehouseDistribution = d.warehouseDistribution
                AND s.warehouseDistributionStatus = :status
                AND s.createdAt >= :start AND s.createdAt < :end
          )
    """)
    long sumReceivedQuantityByDestinationInPeriod(
            @Param("destinationType") LocationType destinationType,
            @Param("destinationId") String destinationId,
            @Param("status") DistributionStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Projection for aggregateLinesByDistribution()
    interface DistributionLineAggregate {
        Long getDistributionId();
        Long getProductsCount();
        Long getTotalQuantity();
        Long getDispatchedQuantity();
        Long getReceivedQuantity();
    }
}
