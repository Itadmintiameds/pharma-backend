package tiameds.pharmabackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-KEY";

    @Value("${internal.api-key:}")
    private String configuredApiKey;

    // Only these paths (ant patterns, without the context path)
    // can be accessed with the API key
    private static final List<String> ALLOWED_API_KEY_PATHS = List.of(
            "/pharmacy/**",
            "/organization/reject/**",
            // publishing the terms & privacy policy is a platform-operator action:
            // it happens before any tenant exists, so there is no user to log in as
            "/admin/terms/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)

            throws ServletException, IOException {

        String requestApiKey = request.getHeader(API_KEY_HEADER);

        if (requestApiKey == null ||
                !isAllowedPath(request.getServletPath()) ||
                SecurityContextHolder
                        .getContext()
                        .getAuthentication() != null) {

            filterChain.doFilter(request, response);

            return;
        }

        if (isValidApiKey(requestApiKey)) {

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(

                            "admin-service",

                            null,

                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_SERVICE")));

            authentication.setDetails(

                    new WebAuthenticationDetailsSource()

                            .buildDetails(request));

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedPath(String path) {

        return ALLOWED_API_KEY_PATHS.stream()
                .anyMatch(pattern ->
                        pathMatcher.match(pattern, path));
    }

    private boolean isValidApiKey(String requestApiKey) {

        if (configuredApiKey == null || configuredApiKey.isBlank()) {

            return false;
        }

        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                configuredApiKey.getBytes(StandardCharsets.UTF_8),
                requestApiKey.getBytes(StandardCharsets.UTF_8));
    }
}
