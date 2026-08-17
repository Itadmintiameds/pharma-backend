package tiameds.pharmabackend.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.context.ClientRequestContext;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.enums.UserAuditAction;

import java.time.LocalDateTime;

/**
 * Single entry point for writing user activity records.
 *
 * Every method is defensive: recording an action must never fail the operation
 * being recorded, so anything unexpected here is swallowed.
 */
@Component
@RequiredArgsConstructor
public class UserAuditRecorder {

    private static final int MAX_DETAILS_LENGTH = 255;

    private final ApplicationEventPublisher eventPublisher;
    private final ClientRequestContext clientRequestContext;
    private final CurrentPharmacyContext pharmacyContext;

    /** An action a user performed on another user. */
    public void record(
            UserAuditAction action,
            UserDetails actor,
            UserDetails target,
            String details) {

        try {
            eventPublisher.publishEvent(new UserAuditEvent(
                    resolveOrganizationId(actor, target),
                    resolvePharmacyId(),
                    action,
                    userId(actor),
                    userName(actor),
                    userId(target),
                    userName(target),
                    truncate(details),
                    clientRequestContext.getIpAddress(),
                    clientRequestContext.getUserAgent(),
                    LocalDateTime.now()));

        } catch (Exception ignored) {
            // Auditing is never allowed to break the operation it describes.
        }
    }

    /** An action a user performed on themselves, such as login or logout. */
    public void recordSelf(
            UserAuditAction action,
            UserDetails actor,
            String details) {

        record(action, actor, actor, details);
    }

    /**
     * An action with no authenticated user, such as a failed login. Only the
     * attempted identifier is known.
     */
    public void recordAnonymous(
            UserAuditAction action,
            String attemptedIdentifier,
            String details) {

        try {
            eventPublisher.publishEvent(new UserAuditEvent(
                    null,
                    resolvePharmacyId(),
                    action,
                    null,
                    attemptedIdentifier,
                    null,
                    attemptedIdentifier,
                    truncate(details),
                    clientRequestContext.getIpAddress(),
                    clientRequestContext.getUserAgent(),
                    LocalDateTime.now()));

        } catch (Exception ignored) {
            // As above.
        }
    }

    private String userId(UserDetails user) {
        return user != null ? user.getUserId() : null;
    }

    private String userName(UserDetails user) {
        return user != null ? user.getFullName() : null;
    }

    /**
     * The organization is a lazy association and the user may be detached, so a
     * failure to resolve it is tolerated rather than propagated.
     */
    private Long resolveOrganizationId(UserDetails actor, UserDetails target) {

        Long organizationId = organizationIdOf(actor);

        return organizationId != null ? organizationId : organizationIdOf(target);
    }

    private Long organizationIdOf(UserDetails user) {

        try {
            if (user == null || user.getOrganization() == null) {
                return null;
            }

            return user.getOrganization().getOrganizationId();

        } catch (Exception e) {
            return null;
        }
    }

    /** Throws when no pharmacy header was sent, which is normal for auth calls. */
    private String resolvePharmacyId() {

        try {
            return pharmacyContext.getCurrentPharmacy();

        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String details) {

        if (details == null) {
            return null;
        }

        return details.length() > MAX_DETAILS_LENGTH
                ? details.substring(0, MAX_DETAILS_LENGTH)
                : details;
    }
}
