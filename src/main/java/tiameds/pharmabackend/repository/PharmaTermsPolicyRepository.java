package tiameds.pharmabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.legal.PharmaTermsPolicy;
import tiameds.pharmabackend.enums.PolicyStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmaTermsPolicyRepository extends JpaRepository<PharmaTermsPolicy, Long> {

    /**
     * The live version. At most one row is ACTIVE — enforced by
     * TermsPolicyService.publish(..), not by the schema.
     */
    Optional<PharmaTermsPolicy> findFirstByStatus(PolicyStatus status);

    Optional<PharmaTermsPolicy> findByVersion(String version);

    boolean existsByVersion(String version);

    List<PharmaTermsPolicy> findAllByOrderByEffectiveFromDesc();
}
