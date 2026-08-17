package tiameds.pharmabackend.dto.audit;

import lombok.Data;
import tiameds.pharmabackend.enums.UserAuditAction;

import java.time.LocalDateTime;

@Data
public class UserAuditLogDto {

    private Long auditId;
    private LocalDateTime createdAt;
    private UserAuditAction action;
    private String actorUserId;
    private String actorName;
    private String targetUserId;
    private String targetName;
    private String details;
    private String ipAddress;
    private String pharmacyId;
}
