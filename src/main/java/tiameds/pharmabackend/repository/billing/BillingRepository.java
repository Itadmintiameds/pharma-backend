package tiameds.pharmabackend.repository.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.Billing;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    List<Billing> findByPharmacy_PharmacyId(String pharmacyId);

    Optional<Billing> findByBillingIdAndPharmacy_PharmacyId(Long billingId, String pharmacyId);
}
