package tiameds.pharmabackend.audit;

import tiameds.pharmabackend.enums.UserAuditAction;

import java.time.LocalDateTime;

/**
 * Fully resolved audit record. Everything is captured on the request thread
 * before publishing, so the listener can run asynchronously without touching
 * lazy entity associations or request-scoped state.
 */
public record UserAuditEvent(
        Long organizationId,
        String pharmacyId,
        UserAuditAction action,
        String actorUserId,
        String actorName,
        String targetUserId,
        String targetName,
        String details,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt) {
}
