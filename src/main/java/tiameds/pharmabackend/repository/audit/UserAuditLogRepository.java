package tiameds.pharmabackend.repository.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.audit.UserAuditLog;

import java.util.List;

/**
 * The filtered listing is built with a Specification rather than a static query:
 * every filter is optional, and emitting "(:param IS NULL OR ...)" leaves the
 * parameter untyped, which PostgreSQL rejects with
 * "could not determine data type of parameter". Building the predicates
 * dynamically also keeps the generated SQL index-friendly.
 */
@Repository
public interface UserAuditLogRepository
        extends JpaRepository<UserAuditLog, Long>,
                JpaSpecificationExecutor<UserAuditLog> {

    /** Distinct actors that appear in the log, for the "Users" filter dropdown. */
    @Query("""
        SELECT DISTINCT a.actorUserId, a.actorName
        FROM UserAuditLog a
        WHERE a.organizationId = :organizationId
          AND a.actorUserId IS NOT NULL
        ORDER BY a.actorName ASC
    """)
    List<Object[]> findDistinctActors(@Param("organizationId") Long organizationId);
}
