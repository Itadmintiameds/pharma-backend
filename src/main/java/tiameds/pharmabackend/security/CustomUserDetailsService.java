package tiameds.pharmabackend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.repository.UserDetailsRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDetailsRepository userRepository;

    @Override
    public User loadUserByUsername(String username)
            throws UsernameNotFoundException {

        tiameds.pharmabackend.entity.UserDetails user =
                userRepository.findByUserEmail(username)
                        .orElseThrow(() ->
                                new UsernameNotFoundException("User not found"));

        return new User(
                user.getUserEmail(),
                user.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().getRoleName()
                        )
                )
        );
    }
}