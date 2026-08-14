package tiameds.pharmabackend.repository.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.audit.UserAuditLog;
import tiameds.pharmabackend.enums.UserAuditAction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    /**
     * Keyset pagination: the caller passes the last row it saw as
     * (cursorCreatedAt, cursorAuditId) instead of an offset, so page 500 costs
     * the same as page 1. All filters are optional and disabled by passing null.
     */
    @Query("""
        SELECT a
        FROM UserAuditLog a
        WHERE a.organizationId = :organizationId
          AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
          AND (:toDate IS NULL OR a.createdAt <= :toDate)
          AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
          AND (:action IS NULL OR a.action = :action)
          AND (
                :cursorCreatedAt IS NULL
                OR a.createdAt < :cursorCreatedAt
                OR (a.createdAt = :cursorCreatedAt AND a.auditId < :cursorAuditId)
              )
        ORDER BY a.createdAt DESC, a.auditId DESC
    """)
    List<UserAuditLog> findPage(
            @Param("organizationId") Long organizationId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("actorUserId") String actorUserId,
            @Param("action") UserAuditAction action,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorAuditId") Long cursorAuditId,
            Pageable pageable
    );

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
