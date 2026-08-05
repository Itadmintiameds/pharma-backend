package tiameds.pharmabackend.repository.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.CustomerManagement;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerManagementRepository extends JpaRepository<CustomerManagement, Long> {

    Optional<CustomerManagement> findByPharmacy_PharmacyIdAndCustomerPhoneNo(
            String pharmacyId,
            String customerPhoneNo
    );

    List<CustomerManagement> findByPharmacy_PharmacyId(String pharmacyId);
}
