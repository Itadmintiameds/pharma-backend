package tiameds.pharmabackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tiameds.pharmabackend.context.ClientRequestContext;

import java.io.IOException;

/**
 * Captures the caller's IP address and user agent into {@link ClientRequestContext}
 * so audit records can carry them. Purely additive: it never blocks a request.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class ClientRequestFilter extends OncePerRequestFilter {

    private static final int MAX_USER_AGENT_LENGTH = 255;

    private final ClientRequestContext clientRequestContext;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            clientRequestContext.set(
                    resolveIpAddress(request),
                    truncate(request.getHeader("User-Agent")));

        } catch (Exception ignored) {
            // Capturing client details must never interfere with the request.
        }

        try {
            filterChain.doFilter(request, response);

        } finally {
            clientRequestContext.clear();
        }
    }

    /**
     * Behind a proxy or load balancer getRemoteAddr() returns the proxy, so the
     * first hop in X-Forwarded-For is preferred when present.
     */
    private String resolveIpAddress(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {

            String firstHop = forwardedFor.split(",")[0].trim();

            if (!firstHop.isEmpty()) {
                return firstHop;
            }
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String truncate(String userAgent) {

        if (userAgent == null) {
            return null;
        }

        return userAgent.length() > MAX_USER_AGENT_LENGTH
                ? userAgent.substring(0, MAX_USER_AGENT_LENGTH)
                : userAgent;
    }
}
