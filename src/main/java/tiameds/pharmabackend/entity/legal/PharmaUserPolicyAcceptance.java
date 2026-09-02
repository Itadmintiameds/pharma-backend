package tiameds.pharmabackend.entity.legal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.UserDetails;

import java.time.LocalDateTime;

/**
 * Consent ledger: one row per (user, policy version) accepted.
 * <p>
 * Append-only. A user accepting a later version produces a new row rather than
 * an update, so the history of what was agreed to and when stays intact. The one
 * sanctioned mutation is the {@code organizationId} backfill — see the field.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "pharma_user_policy_acceptance",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_policy_acceptance",
                columnNames = {"user_id", "policy_id"}),
        indexes = @Index(
                name = "idx_user_policy_acceptance_org",
                columnList = "organization_id"))
public class PharmaUserPolicyAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserDetails user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private PharmaTermsPolicy policy;

    /**
     * Null at registration: the user has no organization yet, because the org is
     * created afterwards by PharmacyOrganizationServiceImpl.createOrganization.
     * Backfilled there. Deliberately a plain column, not a @ManyToOne, so the
     * backfill stays a single bulk UPDATE.
     */
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "accepted", nullable = false)
    private Boolean accepted;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    /** Snapshot, so a consent row can be read and exported without a join. */
    @Column(name = "policy_version", length = 50)
    private String policyVersion;

    /**
     * Best-effort client address. Behind a proxy or CDN this is the first
     * X-Forwarded-For entry, which is client-spoofable unless the proxy
     * overwrites it — see ClientIpResolver in UserDetailsController.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
