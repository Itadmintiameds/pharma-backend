package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.Purchase;

import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByPharmacyId(String pharmacyId);

    List<Purchase> findByWarehouseId(String warehouseId);

    @Query("""
        SELECT p.grnNo
        FROM Purchase p
        WHERE p.grnNo LIKE CONCAT(:prefix, '%')
          AND p.pharmacyId = :pharmacyId
        ORDER BY p.grnNo DESC
    """)
    List<String> findLatestGrn(
            @Param("prefix") String prefix,
            @Param("pharmacyId") String pharmacyId,
            Pageable pageable);

    @Query("""
        SELECT p.grnNo
        FROM Purchase p
        WHERE p.grnNo LIKE CONCAT(:prefix, '%')
          AND p.warehouseId = :warehouseId
        ORDER BY p.grnNo DESC
    """)
    List<String> findLatestGrnByWarehouse(
            @Param("prefix") String prefix,
            @Param("warehouseId") String warehouseId,
            Pageable pageable);

    // A supplier cannot raise the same invoice number twice in the same year.
    // Scoped to the pharmacy, since each pharmacy keeps its own purchase book.
    @Query("""
        SELECT COUNT(p) > 0
        FROM Purchase p
        WHERE p.pharmacyId = :pharmacyId
          AND p.supplier.supplierId = :supplierId
          AND UPPER(TRIM(p.invoiceNo)) = UPPER(TRIM(:invoiceNo))
          AND EXTRACT(YEAR FROM p.invoiceDate) = :year
    """)
    boolean existsBySupplierInvoiceNoAndYear(
            @Param("pharmacyId") String pharmacyId,
            @Param("supplierId") Long supplierId,
            @Param("invoiceNo") String invoiceNo,
            @Param("year") Integer year
    );

    // Same rule as above, scoped to a warehouse, since a warehouse keeps its
    // own purchase book independent of any pharmacy.
    @Query("""
        SELECT COUNT(p) > 0
        FROM Purchase p
        WHERE p.warehouseId = :warehouseId
          AND p.supplier.supplierId = :supplierId
          AND UPPER(TRIM(p.invoiceNo)) = UPPER(TRIM(:invoiceNo))
          AND EXTRACT(YEAR FROM p.invoiceDate) = :year
    """)
    boolean existsBySupplierInvoiceNoAndYearForWarehouse(
            @Param("warehouseId") String warehouseId,
            @Param("supplierId") Long supplierId,
            @Param("invoiceNo") String invoiceNo,
            @Param("year") Integer year
    );

}
