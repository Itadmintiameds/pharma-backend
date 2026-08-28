package tiameds.pharmabackend.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements org.springframework.security.core.userdetails.UserDetails {

    private final UserDetails user;

    /**
     * MODULE/FEATURE/PERMISSION codes granted to this user
     * (see {@link PermissionCodeFormatter}). Exposed as authorities so
     * endpoints can be guarded with
     * {@code @PreAuthorize("hasAuthority('MODULE/FEATURE/PERMISSION')")}.
     */
    private final List<String> permissionCodes;

    public CustomUserDetails(UserDetails user) {
        this(user, List.of());
    }

    public CustomUserDetails(UserDetails user, List<String> permissionCodes) {
        this.user = user;
        this.permissionCodes = permissionCodes != null ? permissionCodes : List.of();
    }

    public String getUserId() {
        return user.getUserId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Role (kept as-is for existing role-based checks)
        authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleName()));

        // Fine-grained permissions (MODULE/FEATURE/PERMISSION)
        for (String code : permissionCodes) {
            authorities.add(new SimpleGrantedAuthority(code));
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUserEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "Active".equalsIgnoreCase(user.getUserStatus());
    }
}