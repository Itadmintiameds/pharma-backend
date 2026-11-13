package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.PharmacyDetails;

import java.util.List;

@Repository
public interface PharmacyDetailsRepository extends JpaRepository<PharmacyDetails, String> {

    @Query("""
       SELECT p.pharmacyId
       FROM PharmacyDetails p
       ORDER BY p.createdAt DESC
       LIMIT 1
       """)
    String findLatestPharmacyId();

    List<PharmacyDetails> findAllByOrganization_OrganizationIdOrderByPharmacyCity(
            Long organizationId);

    List<PharmacyDetails> findAllByOrganization_OrganizationIdInOrderByPharmacyName(
            List<Long> organizationIds);

    @Query("""
    SELECT COUNT(p) > 0
    FROM PharmacyDetails p
    JOIN p.users u
    WHERE p.pharmacyId = :pharmacyId
      AND u.userId = :userId
""")
    boolean existsUserPharmacy(
            @Param("pharmacyId") String pharmacyId,
            @Param("userId") String userId);

    @Query("""
    SELECT p.pharmacyId
    FROM PharmacyDetails p
    JOIN p.users u
    WHERE u.userId = :userId
""")
    List<String> findPharmacyIdsByUserId(@Param("userId") String userId);

    // Whether a pharmacy belonging to a different organization already holds
    // this GSTIN. gstNumber must be passed trimmed and upper-cased; pass -1 to
    // exclude nothing.
    @Query("""
    SELECT COUNT(p) > 0
    FROM PharmacyDetails p
    LEFT JOIN p.organization o
    WHERE UPPER(TRIM(p.gstNumber)) = :gstNumber
      AND (o IS NULL OR o.organizationId <> :excludeOrganizationId)
""")
    boolean existsGstNumberInOtherOrganization(
            @Param("gstNumber") String gstNumber,
            @Param("excludeOrganizationId") Long excludeOrganizationId);

    // Whether any pharmacy, in any organization, already holds this GSTIN.
    // gstNumber must be passed trimmed and upper-cased.
    @Query("""
    SELECT COUNT(p) > 0
    FROM PharmacyDetails p
    WHERE UPPER(TRIM(p.gstNumber)) = :gstNumber
""")
    boolean existsGstNumber(@Param("gstNumber") String gstNumber);
}
