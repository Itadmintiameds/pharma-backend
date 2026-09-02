package tiameds.pharmabackend.enums;

/**
 * Lifecycle of a terms &amp; privacy policy version.
 * <p>
 * At most one row is ACTIVE at a time. That invariant is enforced solely by
 * {@code TermsPolicyService.publish(..)} — the schema cannot express it, because
 * Flyway is disabled and Hibernate's ddl-auto cannot create a partial unique index.
 * Every status transition must therefore go through that method.
 */
public enum PolicyStatus {

    /** Uploaded but not yet served to anyone. */
    DRAFT,

    /** The version currently served by /terms/current. */
    ACTIVE,

    /** Superseded by a newer version. Retained for the acceptance history. */
    ARCHIVED
}
