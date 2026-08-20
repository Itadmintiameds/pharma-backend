package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistribution;
import tiameds.pharmabackend.enums.LocationType;

import java.util.List;

@Repository
public interface WarehouseDistributionRepository extends JpaRepository<WarehouseDistribution, Long> {

    // Distributions this warehouse is involved in, for the list screen: OUTGOING
    // (requested by it) plus INCOMING (it is the destination warehouse). Both sides
    // hit an index (idx_wd_requested_by / idx_wd_destination), so the list never
    // scans the whole table.
    @Query("""
        SELECT d FROM WarehouseDistribution d
        WHERE d.allocationRequestedBy = :warehouseId
           OR (d.destinationType = :warehouseType AND d.destinationId = :warehouseId)
    """)
    List<WarehouseDistribution> findForWarehouse(
            @Param("warehouseId") String warehouseId,
            @Param("warehouseType") LocationType warehouseType,
            Sort sort);

    // KPI: all-time count of distributions requested by a warehouse — index-backed by
    // idx_wd_requested_by.
    long countByAllocationRequestedBy(String allocationRequestedBy);

    // Distributions shipped FROM a store (source side) — index-backed by idx_wd_source.
    List<WarehouseDistribution> findBySourceTypeAndSourceId(
            LocationType sourceType, String sourceId, Sort sort);

    // Distributions shipped TO a store (destination side) — index-backed by idx_wd_destination.
    List<WarehouseDistribution> findByDestinationTypeAndDestinationId(
            LocationType destinationType, String destinationId, Sort sort);

    // Latest allocation number for a given prefix (e.g. "ALC-2026-"), highest first.
    // Allocation numbers run as one global yearly sequence.
    @Query("""
        SELECT d.allocationNo
        FROM WarehouseDistribution d
        WHERE d.allocationNo LIKE CONCAT(:prefix, '%')
        ORDER BY d.allocationNo DESC
    """)
    List<String> findLatestAllocationNo(
            @Param("prefix") String prefix,
            Pageable pageable
    );
}
