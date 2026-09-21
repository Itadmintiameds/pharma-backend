package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.PurchaseReturnDetails;

@Repository
public interface PurchaseReturnDetailsRepository extends JpaRepository<PurchaseReturnDetails, Long> {
}
