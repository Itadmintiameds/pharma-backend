package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.PurchaseReturn;

@Repository
public interface PurchaseReturnRepository extends JpaRepository <PurchaseReturn, Long> {
}
