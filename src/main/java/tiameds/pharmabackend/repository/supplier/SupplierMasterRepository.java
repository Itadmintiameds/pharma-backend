package tiameds.pharmabackend.repository.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;

import java.util.List;

@Repository
public interface SupplierMasterRepository extends JpaRepository<SupplierMaster, Long> {

    List<SupplierMaster> findByPharmacyId(String pharmacyId);

}
