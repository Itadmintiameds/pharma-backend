package tiameds.pharmabackend.repository.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.BillingPayment;

import java.util.List;

@Repository
public interface BillingPaymentRepository extends JpaRepository<BillingPayment, Long> {

    List<BillingPayment> findByBilling_BillingId(Long billingId);
}
