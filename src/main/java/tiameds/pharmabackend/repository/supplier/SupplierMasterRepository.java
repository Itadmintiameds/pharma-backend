package tiameds.pharmabackend.repository.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
