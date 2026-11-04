package tiameds.pharmabackend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Hardcoded passcode for the operator endpoints called by another service: product
 * bulk upload and the admin location directory. Each of those controllers checks the
 * {@link #HEADER} header itself.
 */
public final class OperatorPasscode {

    public static final String HEADER = "X-Upload-Passcode";

    /**
     * Paths (ant patterns, without the context path) authenticated by the passcode
     * alone. SecurityConfig permits them, and JwtAuthenticationFilter skips them so a
     * stray access_token cookie can never turn the call into a logged-in user's.
     */
    public static final List<String> PATHS = List.of(
            "/product/bulk-upload",
            "/product/bulk-upload/**",
            "/admin/organizations"
    );

    private static final byte[] PASSCODE =
            "U0KAURbBi7mWUo8fSlumuLBxMJewYSLz".getBytes(StandardCharsets.UTF_8);

    private OperatorPasscode() {
    }

    // Constant-time comparison so the passcode cannot be guessed by timing.
    public static boolean isValid(String passcode) {
        return passcode != null && MessageDigest.isEqual(
                PASSCODE, passcode.getBytes(StandardCharsets.UTF_8));
    }
}
