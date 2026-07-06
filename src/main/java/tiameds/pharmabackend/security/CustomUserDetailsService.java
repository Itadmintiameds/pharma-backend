package tiameds.pharmabackend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.repository.UserDetailsRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDetailsRepository userRepository;

    @Override
    public CustomUserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        UserDetails user = userRepository.findByUserEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        return new CustomUserDetails(user);
    }
}