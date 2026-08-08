package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, String> {

    Optional<UserDetails> findByUserEmail(String email);

    boolean existsByUserEmail(String userEmail);

    @Query("""
            SELECT u
            FROM UserDetails u
            LEFT JOIN FETCH u.organization
            WHERE u.userId = :userId
            """)
    Optional<UserDetails> findByUserIdWithOrganization(@Param("userId") String userId);

    @Query("""
            SELECT DISTINCT u
            FROM UserDetails u
            LEFT JOIN FETCH u.pharmacies
            WHERE u.organization.organizationId = :organizationId
            """)
    List<UserDetails> findAllByOrganizationIdWithPharmacies(
            @Param("organizationId") Long organizationId);


    @Query("""
    SELECT COUNT(u) > 0
    FROM UserDetails u
    JOIN u.pharmacies p
    WHERE p.pharmacyId = :pharmacyId
      AND u.employeeId = :employeeId
""")
    boolean existsByEmployeeIdAndPharmacyId(
            @Param("employeeId") String employeeId,
            @Param("pharmacyId") String pharmacyId
    );
}
