package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.PurchaseReturn;

import java.util.List;

@Repository
public interface PurchaseReturnRepository extends JpaRepository <PurchaseReturn, Long> {

    // Supplier and invoice are shown on every row of the return list, so they
    // are fetched with the return instead of one lazy load per row.
    @Query("""
        SELECT pr
        FROM PurchaseReturn pr
        LEFT JOIN FETCH pr.purchase p
        LEFT JOIN FETCH p.supplier
        WHERE pr.pharmacyId = :pharmacyId
        ORDER BY pr.purchaseReturnId DESC
    """)
    List<PurchaseReturn> findByPharmacyId(@Param("pharmacyId") String pharmacyId);

    @Query("""
        SELECT pr
        FROM PurchaseReturn pr
        LEFT JOIN FETCH pr.purchase p
        LEFT JOIN FETCH p.supplier
        WHERE pr.warehouseId = :warehouseId
        ORDER BY pr.purchaseReturnId DESC
    """)
    List<PurchaseReturn> findByWarehouseId(@Param("warehouseId") String warehouseId);

    // Incremental return-number sequence, scoped per pharmacy (mirrors
    // PurchaseRepository.findLatestGrn for GRN numbers).
    @Query("""
        SELECT pr.returnNo
        FROM PurchaseReturn pr
        WHERE pr.returnNo LIKE CONCAT(:prefix, '%')
          AND pr.pharmacyId = :pharmacyId
        ORDER BY pr.returnNo DESC
    """)
    List<String> findLatestReturnNo(
            @Param("prefix") String prefix,
            @Param("pharmacyId") String pharmacyId,
            Pageable pageable);

    // Same sequence, scoped per warehouse (mirrors findLatestGrnByWarehouse).
    @Query("""
        SELECT pr.returnNo
        FROM PurchaseReturn pr
        WHERE pr.returnNo LIKE CONCAT(:prefix, '%')
          AND pr.warehouseId = :warehouseId
        ORDER BY pr.returnNo DESC
    """)
    List<String> findLatestReturnNoByWarehouse(
            @Param("prefix") String prefix,
            @Param("warehouseId") String warehouseId,
            Pageable pageable);
}
