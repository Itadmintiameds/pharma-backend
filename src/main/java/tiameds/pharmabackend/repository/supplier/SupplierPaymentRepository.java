package tiameds.pharmabackend.repository.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.pharmabackend.entity.supplier.SupplierPayment;

import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    List<SupplierPayment> findByPurchase_PurchaseId(Long purchaseId);

    List<SupplierPayment> findByPurchase_PurchaseIdAndIsActiveTrue(Long purchaseId);
}
