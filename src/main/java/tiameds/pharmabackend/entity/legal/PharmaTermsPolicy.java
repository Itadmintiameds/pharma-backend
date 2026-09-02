package tiameds.pharmabackend.entity.legal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.enums.PolicyStatus;

import java.time.LocalDateTime;

/**
 * One published version of the combined Terms &amp; Conditions / Privacy Policy
 * document. The document itself lives in S3; this table holds only metadata.
 * Rows are never deleted — the acceptance ledger points at them.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "pharma_terms_conditions_policies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_terms_policy_version",
                columnNames = "version"))
public class PharmaTermsPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Publisher-chosen label, e.g. "v1.0". Unique, and used in the S3 key. */
    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "title")
    private String title;

    @Column(name = "document_url", nullable = false, length = 500)
    private String documentUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    /**
     * SHA-256 of the uploaded bytes, so the S3 object can be re-verified later
     * and an accidental replacement detected.
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PolicyStatus status;

    /**
     * Reserved for re-acceptance gating, which is not built yet. Marks a version
     * whose changes are material enough that existing users must accept again.
     */
    @Column(name = "requires_reacceptance", nullable = false)
    private Boolean requiresReacceptance = Boolean.FALSE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** user_id of the admin who uploaded this version. */
    @Column(name = "created_by", length = 20)
    private String createdBy;
}
