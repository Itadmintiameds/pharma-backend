package tiameds.pharmabackend.repository.supplier;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tiameds.pharmabackend.entity.supplier.SupplierPayment;

import java.util.List;
import java.util.Optional;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    List<SupplierPayment> findByPurchase_PurchaseId(Long purchaseId);

    List<SupplierPayment> findByPurchase_PharmacyId(String pharmacyId);

    List<SupplierPayment> findByPurchase_WarehouseId(String warehouseId);

    Optional<SupplierPayment> findBySupplierPaymentIdAndPurchase_PharmacyId(
            Long supplierPaymentId,
            String pharmacyId);

    Optional<SupplierPayment> findBySupplierPaymentIdAndPurchase_WarehouseId(
            Long supplierPaymentId,
            String warehouseId);

    // Incremental supplier-payment-number sequence, scoped per pharmacy (mirrors
    // StockAdjustmentRepository.findLatestStockAdjustmentNo).
    @Query("""
        SELECT sp.supplierPaymentNo
        FROM SupplierPayment sp
        WHERE sp.supplierPaymentNo LIKE CONCAT(:prefix, '%')
          AND sp.purchase.pharmacyId = :pharmacyId
        ORDER BY sp.supplierPaymentNo DESC
    """)
    List<String> findLatestSupplierPaymentNo(
            @Param("prefix") String prefix,
            @Param("pharmacyId") String pharmacyId,
            Pageable pageable);

    // Same sequence, scoped per warehouse.
    @Query("""
        SELECT sp.supplierPaymentNo
        FROM SupplierPayment sp
        WHERE sp.supplierPaymentNo LIKE CONCAT(:prefix, '%')
          AND sp.purchase.warehouseId = :warehouseId
        ORDER BY sp.supplierPaymentNo DESC
    """)
    List<String> findLatestSupplierPaymentNoByWarehouse(
            @Param("prefix") String prefix,
            @Param("warehouseId") String warehouseId,
            Pageable pageable);

    // Paid amount per purchase, used by the supplier payment KPIs.
    // Each row is [purchaseId, sum of paymentAmount].
    @Query("""
        SELECT sp.purchase.purchaseId, COALESCE(SUM(sp.paymentAmount), 0)
        FROM SupplierPayment sp
        WHERE sp.purchase.pharmacyId = :pharmacyId
        GROUP BY sp.purchase.purchaseId
    """)
    List<Object[]> sumPaymentAmountPerPurchaseByPharmacyId(@Param("pharmacyId") String pharmacyId);

    @Query("""
        SELECT sp.purchase.purchaseId, COALESCE(SUM(sp.paymentAmount), 0)
        FROM SupplierPayment sp
        WHERE sp.purchase.warehouseId = :warehouseId
        GROUP BY sp.purchase.purchaseId
    """)
    List<Object[]> sumPaymentAmountPerPurchaseByWarehouseId(@Param("warehouseId") String warehouseId);
}
