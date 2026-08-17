package tiameds.pharmabackend.enums;

/**
 * Actions recorded in the user management activity log.
 */
public enum UserAuditAction {
    USER_CREATED,
    USER_UPDATED,
    USER_STATUS_CHANGED,
    PERMISSIONS_UPDATED,
    PASSWORD_SET,
    PASSWORD_RESET,
    LOGIN,
    LOGIN_FAILED,
    LOGOUT
}
