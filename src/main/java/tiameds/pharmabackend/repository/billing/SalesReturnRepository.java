package tiameds.pharmabackend.repository.billing;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.SalesReturn;
import tiameds.pharmabackend.enums.SalesReturnStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {

    // The bill and its customer are shown on every row of the return list, so
    // they are fetched with the return instead of one lazy load per row.
    @Query("""
        SELECT sr
        FROM SalesReturn sr
        LEFT JOIN FETCH sr.billing b
        LEFT JOIN FETCH b.customer
        WHERE sr.pharmacyId = :pharmacyId
        ORDER BY sr.salesReturnId DESC
    """)
    List<SalesReturn> findByPharmacyId(@Param("pharmacyId") String pharmacyId);

    Optional<SalesReturn> findBySalesReturnIdAndPharmacyId(Long salesReturnId, String pharmacyId);

    long countByPharmacyIdAndSalesReturnStatus(String pharmacyId, SalesReturnStatus status);

    @Query("""
        SELECT COALESCE(SUM(sr.totalNetAmount), 0)
        FROM SalesReturn sr
        WHERE sr.pharmacyId = :pharmacyId
          AND sr.salesReturnStatus = :status
    """)
    BigDecimal sumTotalNetAmount(
            @Param("pharmacyId") String pharmacyId,
            @Param("status") SalesReturnStatus status);

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
