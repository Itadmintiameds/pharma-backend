package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.PurchaseReturnDetails;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;

import java.util.List;

@Repository
public interface PurchaseReturnDetailsRepository extends JpaRepository<PurchaseReturnDetails, Long> {

    /**
     * Every line that actually counts as returned against a purchase.
     *
     * <p>Two filters carry the meaning: only returns in the given status (the
     * caller passes CONFIRMED, since a draft has not sent anything back), and
     * only the latest revision of each line — a superseded revision is history,
     * not a quantity. Rows written before revisioning have a null flag and count
     * as current.
     */
    @Query("""
        SELECT prd
        FROM PurchaseReturnDetails prd
        WHERE prd.purchaseReturn.purchase.purchaseId = :purchaseId
          AND prd.purchaseReturn.status = :status
          AND (prd.isActive IS NULL OR prd.isActive = true)
    """)
    List<PurchaseReturnDetails> findCountableLines(
            @Param("purchaseId") Long purchaseId,
            @Param("status") PurchaseReturnStatus status);
}
