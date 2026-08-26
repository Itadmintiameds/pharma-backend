package tiameds.pharmabackend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Central authorization check used from {@code @PreAuthorize} expressions,
 * e.g. {@code @PreAuthorize("@access.has('PURCHASE/PURCHASE/VIEW')")}.
 * <p>
 * SUPER ADMIN bypasses every permission check and is granted access to all
 * endpoints; every other role is gated by the exact MODULE/FEATURE/PERMISSION
 * code (see {@link PermissionCodeFormatter}). Keeping the bypass here means it
 * lives in one place instead of being repeated on every annotation.
 * <p>
 * Note: in Spring Security 7 the built-in {@code hasAuthority}/{@code hasRole}
 * expression methods are {@code final}, so a super-admin bypass cannot be added
 * by overriding them — a bean expression like this is the supported approach.
 */
@Component("access")
public class AccessChecker {

    /**
     * Exact role name as stored in pharma_roles.role_name.
     */
    private static final String SUPER_ADMIN_ROLE = "SUPER ADMIN";

    /**
     * @return true if the current user is SUPER ADMIN, or holds {@code permissionCode}.
     */
    public boolean has(String permissionCode) {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        for (GrantedAuthority authority : auth.getAuthorities()) {
            String value = authority.getAuthority();
            if (SUPER_ADMIN_ROLE.equals(value) || permissionCode.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the current user is SUPER ADMIN, or holds any of {@code permissionCodes}.
     */
    public boolean hasAny(String... permissionCodes) {
        for (String code : permissionCodes) {
            if (has(code)) {
                return true;
            }
        }
        return false;
    }
}
