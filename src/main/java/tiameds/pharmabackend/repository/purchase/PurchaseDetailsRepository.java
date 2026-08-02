package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;

@Repository
public interface PurchaseDetailsRepository extends JpaRepository<PurchaseDetails, Long> {
}
