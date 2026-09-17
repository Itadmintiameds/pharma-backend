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
            "/admin/terms/**",
            // onboarding a tenant's catalogue is run by an operator/migration script
            // rather than from a logged-in session. There is no user to resolve a
            // location from, so these endpoints take the pharmacy/warehouse id explicitly.
            "/product/bulk-upload",
            "/product/bulk-upload/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * The key is configured through a chain of fallbacks that all fail silently, ending
     * at the literal "dummy" — which then rejects every correct key with a bare 401.
     * Report at startup whether a real key was picked up. The key itself is never logged.
     */
    @jakarta.annotation.PostConstruct
    void reportApiKeyConfiguration() {

        if (configuredApiKey == null || configuredApiKey.isBlank()) {

            logger.warn("internal.api-key is not configured — API key authentication is disabled");

        } else if ("dummy".equals(configuredApiKey)) {

            logger.warn("internal.api-key fell back to the placeholder \"dummy\" — "
                    + "INTERNAL_API_KEY was not resolved from .env or the environment");

        } else {

            logger.info("internal.api-key configured (" + configuredApiKey.length() + " chars)");
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)

            throws ServletException, IOException {

        String requestApiKey = request.getHeader(API_KEY_HEADER);

        String path = pathWithinApplication(request);

        if (requestApiKey == null ||
                !isAllowedPath(path) ||
                SecurityContextHolder
                        .getContext()
                        .getAuthentication() != null) {

            // Every rejection here ends as a bare 401 from the entry point, which tells
            // the caller nothing about which of the three conditions tripped. Log it
            // whenever a key was actually offered, so a failing integration is diagnosable.
            if (requestApiKey != null) {

                logger.warn("API key offered but not used for "
                        + request.getMethod() + " " + path
                        + " — pathAllowed=" + isAllowedPath(path)
                        + ", alreadyAuthenticated="
                        + (SecurityContextHolder.getContext().getAuthentication() != null)
                        + " [uri=" + request.getRequestURI()
                        + ", contextPath=" + request.getContextPath()
                        + ", servletPath=" + request.getServletPath() + "]");
            }

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

        } else {

            // Never log either key. Lengths alone separate "wrong key" from
            // "key never reached the app" (configured length 0 = .env not loaded).
            logger.warn("API key rejected for " + request.getMethod() + " "
                    + request.getServletPath()
                    + " — offered length " + requestApiKey.length()
                    + ", configured length "
                    + (configuredApiKey == null ? 0 : configuredApiKey.length()));
        }

        filterChain.doFilter(request, response);
    }

    /**
     * The request path as the allowlist patterns are written — leading slash, no
     * context path, no query string.
     * <p>
     * Derived from the request URI rather than taken from {@code getServletPath()}:
     * for a servlet mapped to "/" that method's value is container-specific (and is
     * deprecated as of Servlet 6.1), so relying on it makes the allowlist silently
     * stop matching when the container or context path changes. Subtracting the
     * context path from the URI is well defined everywhere.
     */
    private String pathWithinApplication(HttpServletRequest request) {

        String uri = request.getRequestURI();

        if (uri == null) {

            return "/";
        }

        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {

            uri = uri.substring(contextPath.length());
        }

        // A trailing slash would stop an exact pattern such as "/product/bulk-upload"
        // from matching a request to "/product/bulk-upload/".
        if (uri.length() > 1 && uri.endsWith("/")) {

            uri = uri.substring(0, uri.length() - 1);
        }

        return uri.isEmpty() ? "/" : uri;
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
