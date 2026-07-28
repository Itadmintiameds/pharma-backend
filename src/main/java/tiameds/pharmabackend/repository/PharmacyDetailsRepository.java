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

    @Query("""
    SELECT COUNT(p) > 0
    FROM PharmacyDetails p
    JOIN p.users u
    WHERE p.pharmacyId = :pharmacyId
      AND u.userId = :userId
""")
    boolean existsUserPharmacy(
            @Param("pharmacyId") String pharmacyId,
            @Param("userId") Long userId);

    @Query("""
    SELECT p.pharmacyId
    FROM PharmacyDetails p
    JOIN p.users u
    WHERE u.userId = :userId
""")
    List<String> findPharmacyIdsByUserId(@Param("userId") Long userId);
}
