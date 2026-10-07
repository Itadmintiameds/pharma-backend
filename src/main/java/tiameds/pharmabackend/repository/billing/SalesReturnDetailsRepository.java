package tiameds.pharmabackend.repository.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.SalesReturnDetails;
import tiameds.pharmabackend.enums.SalesReturnStatus;

import java.util.List;

@Repository
public interface SalesReturnDetailsRepository extends JpaRepository<SalesReturnDetails, Long> {

    // Quantity already returned against a bill, per product + batch, counting
    // only returns in the given status. Each row is
    // [productId (String), batchId (String), returnedQuantity (Long)].
    @Query("""
        SELECT d.product.productId, d.batch.batchId, SUM(d.salesReturnQuantity)
        FROM SalesReturnDetails d
        WHERE d.salesReturn.billing.billingId = :billingId
          AND d.salesReturn.salesReturnStatus = :status
        GROUP BY d.product.productId, d.batch.batchId
    """)
    List<Object[]> sumReturnedQuantityByProductAndBatch(
            @Param("billingId") Long billingId,
            @Param("status") SalesReturnStatus status);
}
