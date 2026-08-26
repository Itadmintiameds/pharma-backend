package tiameds.pharmabackend.security;

import tiameds.pharmabackend.entity.UserFeaturePermission;

/**
 * Builds the canonical permission authority string used across the app.
 * <p>
 * Format: {@code MODULE/FEATURE/PERMISSION}
 * (e.g. {@code PURCHASE/PURCHASE/VIEW},
 * {@code WAREHOUSE_DISTRIBUTION/WAREHOUSE_DISTRIBUTION/VIEW}).
 * <p>
 * The same string is exposed as a Spring Security authority
 * (see {@link CustomUserDetails}) and returned to the client
 * (see the current-user permissions endpoint), so both enforcement
 * and the UI share one source of truth.
 */
public final class PermissionCodeFormatter {

    private PermissionCodeFormatter() {
    }

    /**
     * Formats a granted {@link UserFeaturePermission} row as
     * {@code MODULE/FEATURE/PERMISSION}.
     */
    public static String format(UserFeaturePermission row) {
        return normalize(row.getFeature().getModule().getModuleName())
                + "/"
                + row.getFeature().getFeatureCode()
                + "/"
                + normalize(row.getPermission().getPermissionName());
    }

    /**
     * Normalizes a name/code into an uppercase, underscore-separated token
     * (e.g. "Warehouse Distribution" -> "WAREHOUSE_DISTRIBUTION").
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase().replace(' ', '_');
    }
}
