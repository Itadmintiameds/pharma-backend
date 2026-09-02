package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.legal.PharmaUserPolicyAcceptance;

import java.util.List;

@Repository
public interface PharmaUserPolicyAcceptanceRepository
        extends JpaRepository<PharmaUserPolicyAcceptance, Long> {

    boolean existsByUser_UserIdAndPolicy_Id(String userId, Long policyId);

    List<PharmaUserPolicyAcceptance> findByUser_UserIdOrderByAcceptedAtDesc(String userId);

    List<PharmaUserPolicyAcceptance> findByOrganizationId(Long organizationId);

    /**
     * Stamps the organization onto the acceptance rows a user recorded before the
     * organization existed. Only fills nulls, so it can never rewrite a row that
     * was already attributed to a different organization.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PharmaUserPolicyAcceptance a
               SET a.organizationId = :organizationId
             WHERE a.user.userId = :userId
               AND a.organizationId IS NULL
            """)
    int backfillOrganizationId(
            @Param("userId") String userId,
            @Param("organizationId") Long organizationId);
}
