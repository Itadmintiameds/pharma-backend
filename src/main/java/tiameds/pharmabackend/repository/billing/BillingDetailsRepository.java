package tiameds.pharmabackend.repository.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.BillingDetails;

import java.util.List;

@Repository
public interface BillingDetailsRepository extends JpaRepository<BillingDetails, Long> {

    List<BillingDetails> findByBilling_BillingId(Long billingId);
}
