package tiameds.pharmabackend.repository.billing;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.SalesReturn;

import java.util.List;

@Repository
public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {

    // Sales return numbers run as their own sequence per pharmacy, so each
    // pharmacy gets SLR-<year>-00001 onwards independently of the others.
    @Query("""
        SELECT sr.salesReturnNo
        FROM SalesReturn sr
        WHERE sr.salesReturnNo LIKE CONCAT(:prefix, '%')
          AND sr.pharmacyId = :pharmacyId
        ORDER BY sr.salesReturnNo DESC
    """)
    List<String> findLatestSalesReturnNo(
            @Param("prefix") String prefix,
            @Param("pharmacyId") String pharmacyId,
            Pageable pageable);
}
