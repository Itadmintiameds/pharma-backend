package tiameds.pharmabackend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Hardcoded passcode for the operator endpoints that are open in SecurityConfig
 * (no JWT): product bulk upload and the admin location directory. Each of those
 * controllers checks the {@link #HEADER} header itself.
 */
public final class OperatorPasscode {

    public static final String HEADER = "X-Upload-Passcode";

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
