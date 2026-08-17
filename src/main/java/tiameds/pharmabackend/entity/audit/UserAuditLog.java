package tiameds.pharmabackend.entity.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.enums.UserAuditAction;

import java.time.LocalDateTime;

/**
 * Append-only activity log for user management. Rows are never updated or
 * deleted, and actor/target names are stored as snapshots so the log keeps
 * reading correctly after a user is renamed, and so the listing needs no joins.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "pharma_user_audit_log",
        indexes = {
                @Index(
                        name = "idx_user_audit_org_time",
                        columnList = "organization_id, created_at"),
                @Index(
                        name = "idx_user_audit_org_actor_time",
                        columnList = "organization_id, actor_user_id, created_at"),
                @Index(
                        name = "idx_user_audit_org_action_time",
                        columnList = "organization_id, action, created_at")
        }
)
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "pharmacy_id", length = 30)
    private String pharmacyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 40)
    private UserAuditAction action;

    @Column(name = "actor_user_id", length = 20)
    private String actorUserId;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "target_user_id", length = 20)
    private String targetUserId;

    @Column(name = "target_name")
    private String targetName;

    @Column(name = "details", length = 255)
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
