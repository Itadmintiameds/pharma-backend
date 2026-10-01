package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.WarehouseReturn;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseReturnRepository extends JpaRepository<WarehouseReturn, Long> {

    // Lines, products and batches are shown with every return, so they are
    // fetched with it instead of one lazy load per row.
    @Query("""
        SELECT DISTINCT wr
        FROM WarehouseReturn wr
        LEFT JOIN FETCH wr.warehouseReturnDetails d
        LEFT JOIN FETCH d.product
        LEFT JOIN FETCH d.batch
        WHERE wr.fromPharmacyId = :pharmacyId
          AND (wr.isDelete IS NULL OR wr.isDelete = false)
        ORDER BY wr.warehouseReturnId DESC
    """)
    List<WarehouseReturn> findByFromPharmacyId(@Param("pharmacyId") String pharmacyId);

    @Query("""
        SELECT DISTINCT wr
        FROM WarehouseReturn wr
        LEFT JOIN FETCH wr.warehouseReturnDetails d
        LEFT JOIN FETCH d.product
        LEFT JOIN FETCH d.batch
        WHERE wr.toWarehouseId = :warehouseId
          AND (wr.isDelete IS NULL OR wr.isDelete = false)
        ORDER BY wr.warehouseReturnId DESC
    """)
    List<WarehouseReturn> findByToWarehouseId(@Param("warehouseId") String warehouseId);

    @Query("""
        SELECT wr
        FROM WarehouseReturn wr
        LEFT JOIN FETCH wr.warehouseReturnDetails d
        LEFT JOIN FETCH d.product
        LEFT JOIN FETCH d.batch
        WHERE wr.warehouseReturnId = :warehouseReturnId
          AND (wr.isDelete IS NULL OR wr.isDelete = false)
    """)
    Optional<WarehouseReturn> findActiveByIdWithDetails(@Param("warehouseReturnId") Long warehouseReturnId);

    // Incremental stock-return-number sequence, scoped per pharmacy (mirrors
    // PurchaseReturnRepository.findLatestReturnNo).
    @Query("""
        SELECT wr.stockReturnNo
        FROM WarehouseReturn wr
        WHERE wr.stockReturnNo LIKE CONCAT(:prefix, '%')
          AND wr.fromPharmacyId = :pharmacyId
        ORDER BY wr.stockReturnNo DESC
    """)
    List<String> findLatestStockReturnNo(
            @Param("prefix") String prefix,
            @Param("pharmacyId") String pharmacyId,
            Pageable pageable);
}
