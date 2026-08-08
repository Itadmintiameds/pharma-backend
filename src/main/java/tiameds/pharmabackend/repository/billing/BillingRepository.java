package tiameds.pharmabackend.repository.billing;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.Billing;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    List<Billing> findByPharmacy_PharmacyId(String pharmacyId);

    Optional<Billing> findByBillingIdAndPharmacy_PharmacyId(Long billingId, String pharmacyId);

    // Bill numbers run as their own sequence per pharmacy, so each pharmacy gets
    // BILL-<year>-00001 onwards independently of the others.
    @Query("""
        SELECT b.billNo
        FROM Billing b
        WHERE b.billNo LIKE CONCAT(:prefix, '%')
          AND b.pharmacy.pharmacyId = :pharmacyId
        ORDER BY b.billNo DESC
    """)
    List<String> findLatestBillNo(
            @Param("prefix") String prefix,
            @Param("pharmacyId") String pharmacyId,
            Pageable pageable
    );

    Optional<Billing> findByBillNoAndPharmacy_PharmacyId(String billNo, String pharmacyId);
}
