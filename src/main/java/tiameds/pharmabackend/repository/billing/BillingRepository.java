package tiameds.pharmabackend.repository.billing;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.billing.Billing;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    List<Billing> findByPharmacy_PharmacyId(String pharmacyId);

    List<Billing> findByPharmacy_PharmacyIdAndCustomer_CustomerPhoneNoOrderByBillingIdDesc(
            String pharmacyId,
            String customerPhoneNo);

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

    // Locks the bill row until the transaction commits, so two sales returns
    // against the same bill cannot both read the same already-returned
    // quantities and together return more than was sold.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM Billing b
        WHERE b.billingId = :billingId
          AND b.pharmacy.pharmacyId = :pharmacyId
    """)
    Optional<Billing> findForUpdate(
            @Param("billingId") Long billingId,
            @Param("pharmacyId") String pharmacyId);
}
