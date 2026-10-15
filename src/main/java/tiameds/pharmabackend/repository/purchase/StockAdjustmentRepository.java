package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.StockAdjustment;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    // Product and batch are shown on every row of the adjustment list, so they
    // are fetched with the adjustment instead of one lazy load per row.
    @Query("""
        SELECT sa
        FROM StockAdjustment sa
        LEFT JOIN FETCH sa.product
        LEFT JOIN FETCH sa.batch
        WHERE sa.pharmacyId = :pharmacyId
        ORDER BY sa.stockAdjustmentId DESC
    """)
    List<StockAdjustment> findByPharmacyId(@Param("pharmacyId") String pharmacyId);

    @Query("""
        SELECT sa
        FROM StockAdjustment sa
        LEFT JOIN FETCH sa.product
        LEFT JOIN FETCH sa.batch
        WHERE sa.warehouseId = :warehouseId
        ORDER BY sa.stockAdjustmentId DESC
    """)
    List<StockAdjustment> findByWarehouseId(@Param("warehouseId") String warehouseId);

    long countByPharmacyId(String pharmacyId);

    long countByWarehouseId(String warehouseId);

    @Query("""
        SELECT COALESCE(SUM(sa.adjustmentValue), 0)
        FROM StockAdjustment sa
        WHERE sa.pharmacyId = :pharmacyId
    """)
    Long sumAdjustmentValueByPharmacyId(@Param("pharmacyId") String pharmacyId);

    @Query("""
        SELECT COALESCE(SUM(sa.adjustmentValue), 0)
        FROM StockAdjustment sa
        WHERE sa.warehouseId = :warehouseId
    """)
    Long sumAdjustmentValueByWarehouseId(@Param("warehouseId") String warehouseId);

    // Looked up together with the location, so an adjustment from another
    // pharmacy/warehouse reads as not found.
    @Query("""
        SELECT sa
        FROM StockAdjustment sa
        LEFT JOIN FETCH sa.product
        LEFT JOIN FETCH sa.batch
        WHERE sa.stockAdjustmentId = :stockAdjustmentId
          AND sa.pharmacyId = :pharmacyId
    """)
    Optional<StockAdjustment> findByStockAdjustmentIdAndPharmacyId(
            @Param("stockAdjustmentId") Long stockAdjustmentId,
            @Param("pharmacyId") String pharmacyId);

    @Query("""
        SELECT sa
        FROM StockAdjustment sa
        LEFT JOIN FETCH sa.product
        LEFT JOIN FETCH sa.batch
        WHERE sa.stockAdjustmentId = :stockAdjustmentId
          AND sa.warehouseId = :warehouseId
    """)
    Optional<StockAdjustment> findByStockAdjustmentIdAndWarehouseId(
            @Param("stockAdjustmentId") Long stockAdjustmentId,
            @Param("warehouseId") String warehouseId);

    // Incremental adjustment-number sequence, scoped per pharmacy (mirrors
    // PurchaseReturnRepository.findLatestReturnNo).
    @Query("""
        SELECT sa.stockAdjustmentNo
        FROM StockAdjustment sa
        WHERE sa.stockAdjustmentNo LIKE CONCAT(:prefix, '%')
          AND sa.pharmacyId = :pharmacyId
        ORDER BY sa.stockAdjustmentNo DESC
    """)
    List<String> findLatestStockAdjustmentNo(
            @Param("prefix") String prefix,
            @Param("pharmacyId") String pharmacyId,
            Pageable pageable);

    // Same sequence, scoped per warehouse.
    @Query("""
        SELECT sa.stockAdjustmentNo
        FROM StockAdjustment sa
        WHERE sa.stockAdjustmentNo LIKE CONCAT(:prefix, '%')
          AND sa.warehouseId = :warehouseId
        ORDER BY sa.stockAdjustmentNo DESC
    """)
    List<String> findLatestStockAdjustmentNoByWarehouse(
            @Param("prefix") String prefix,
            @Param("warehouseId") String warehouseId,
            Pageable pageable);
}
