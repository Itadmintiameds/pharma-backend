package tiameds.pharmabackend.repository.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierMasterRepository extends JpaRepository<SupplierMaster, Long> {

    List<SupplierMaster> findByPharmacyId(String pharmacyId);

    Optional<SupplierMaster> findBySupplierIdAndPharmacyId(
            Long supplierId,
            String pharmacyId);

    List<SupplierMaster> findByWarehouseId(String warehouseId);

    Optional<SupplierMaster> findBySupplierIdAndWarehouseId(
            Long supplierId,
            String warehouseId);

    // Other suppliers of the pharmacy sharing any of the given GST / DL / PAN / FSSAI
    // numbers (compared trimmed and case-insensitive). Values must be passed trimmed
    // and upper-cased; a null value matches nothing. excludeSupplierId skips the
    // supplier being edited (pass -1 on create).
    @Query("""
        SELECT s
        FROM SupplierMaster s
        WHERE s.pharmacyId = :pharmacyId
          AND s.supplierId <> :excludeSupplierId
          AND (UPPER(TRIM(s.gstinNo)) = :gstinNo
               OR UPPER(TRIM(s.dlno)) = :dlno
               OR UPPER(TRIM(s.panNo)) = :panNo
               OR UPPER(TRIM(s.fssaiNo)) = :fssaiNo)
    """)
    List<SupplierMaster> findDuplicatesInPharmacy(
            @Param("pharmacyId") String pharmacyId,
            @Param("excludeSupplierId") Long excludeSupplierId,
            @Param("gstinNo") String gstinNo,
            @Param("dlno") String dlno,
            @Param("panNo") String panNo,
            @Param("fssaiNo") String fssaiNo);

    // Same check, scoped per warehouse.
    @Query("""
        SELECT s
        FROM SupplierMaster s
        WHERE s.warehouseId = :warehouseId
          AND s.supplierId <> :excludeSupplierId
          AND (UPPER(TRIM(s.gstinNo)) = :gstinNo
               OR UPPER(TRIM(s.dlno)) = :dlno
               OR UPPER(TRIM(s.panNo)) = :panNo
               OR UPPER(TRIM(s.fssaiNo)) = :fssaiNo)
    """)
    List<SupplierMaster> findDuplicatesInWarehouse(
            @Param("warehouseId") String warehouseId,
            @Param("excludeSupplierId") Long excludeSupplierId,
            @Param("gstinNo") String gstinNo,
            @Param("dlno") String dlno,
            @Param("panNo") String panNo,
            @Param("fssaiNo") String fssaiNo);
}
