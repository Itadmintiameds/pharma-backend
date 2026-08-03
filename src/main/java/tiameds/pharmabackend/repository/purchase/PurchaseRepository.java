package tiameds.pharmabackend.repository.purchase;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.purchase.Purchase;

import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByPharmacyId(String pharmacyId);

    @Query("""
        SELECT p.grnNo
        FROM Purchase p
        WHERE p.grnNo LIKE CONCAT(:prefix, '%')
        ORDER BY p.grnNo DESC
    """)
    List<String> findLatestGrn(@Param("prefix") String prefix, Pageable pageable);

}
