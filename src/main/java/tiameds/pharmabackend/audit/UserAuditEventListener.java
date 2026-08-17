package tiameds.pharmabackend.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tiameds.pharmabackend.entity.audit.UserAuditLog;
import tiameds.pharmabackend.repository.audit.UserAuditLogRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuditEventListener {

    private final UserAuditLogRepository userAuditLogRepository;

    /**
     * AFTER_COMMIT so a rolled back operation leaves no record of having happened.
     * fallbackExecution keeps auth flows working, where there may be no
     * transaction at all. Runs off the request thread and writes in its own
     * transaction; failures are logged and dropped.
     */
    @Async("auditExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserAuditEvent(UserAuditEvent event) {

        try {
            UserAuditLog entity = new UserAuditLog();

            entity.setOrganizationId(event.organizationId());
            entity.setPharmacyId(event.pharmacyId());
            entity.setAction(event.action());
            entity.setActorUserId(event.actorUserId());
            entity.setActorName(event.actorName());
            entity.setTargetUserId(event.targetUserId());
            entity.setTargetName(event.targetName());
            entity.setDetails(event.details());
            entity.setIpAddress(event.ipAddress());
            entity.setUserAgent(event.userAgent());
            entity.setCreatedAt(event.createdAt());

            userAuditLogRepository.save(entity);

        } catch (Exception e) {
            log.warn("Failed to write user audit log for action {}", event.action(), e);
        }
    }
}
