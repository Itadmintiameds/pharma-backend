package tiameds.pharmabackend.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements org.springframework.security.core.userdetails.UserDetails {

    private final UserDetails user;

    public CustomUserDetails(UserDetails user) {
        this.user = user;
    }

    public Long getUserId() {
        return user.getUserId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(user.getRole().getRoleName())
        );
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