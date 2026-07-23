package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.Purchase;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
}
