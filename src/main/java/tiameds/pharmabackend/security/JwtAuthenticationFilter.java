package tiameds.pharmabackend.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tiameds.pharmabackend.service.impl.JwtService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UserDetailsService userDetailsService;

    private final CookieUtil cookieUtil;

    @Override
    protected void doFilterInternal(


            HttpServletRequest request,

            HttpServletResponse response,

            FilterChain filterChain)

            throws ServletException, IOException {

        String token =
                cookieUtil.getCookie(
                        request,
                        "access_token");

        if (token == null) {

            filterChain.doFilter(request, response);

            return;
        }

        try {

            String username =
                    jwtService.extractUsername(token);
            System.out.println("Username = " + username);
            if (username != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                org.springframework.security.core.userdetails.UserDetails
                        userDetails =
                        userDetailsService.loadUserByUsername(username);

                if (jwtService.validateToken(
                        token,
                        userDetails)) {


                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(

                                    userDetails,

                                    null,

                                    userDetails.getAuthorities());

                    authentication.setDetails(

                            new WebAuthenticationDetailsSource()

                                    .buildDetails(request));

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (JwtException ex) {

            SecurityContextHolder.clearContext();

        } catch (Exception ex) {

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}