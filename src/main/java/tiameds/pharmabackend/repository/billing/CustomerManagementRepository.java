package tiameds.pharmabackend.repository.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.CustomerManagement;

import java.util.List;

@Repository
public interface CustomerManagementRepository extends JpaRepository<CustomerManagement, Long> {

    // One phone number can belong to several people (family members sharing a
    // number), so a phone lookup returns every customer registered against it.
    List<CustomerManagement> findByPharmacy_PharmacyIdAndCustomerPhoneNo(
            String pharmacyId,
            String customerPhoneNo
    );

    // A customer is identified by phone + name together.
    List<CustomerManagement> findByPharmacy_PharmacyIdAndCustomerPhoneNoAndCustomerNameIgnoreCase(
            String pharmacyId,
            String customerPhoneNo,
            String customerName
    );

    List<CustomerManagement> findByPharmacy_PharmacyId(String pharmacyId);
}
