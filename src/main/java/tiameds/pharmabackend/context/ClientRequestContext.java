package tiameds.pharmabackend.context;

import org.springframework.stereotype.Component;

/**
 * Carries the caller's IP address and user agent for the duration of a request,
 * so services can record them without taking HttpServletRequest as a parameter.
 * Mirrors how {@link CurrentPharmacyContext} works.
 */
@Component
public class ClientRequestContext {

    private final ThreadLocal<String> ipAddress = new ThreadLocal<>();

    private final ThreadLocal<String> userAgent = new ThreadLocal<>();

    public void set(String ip, String agent) {
        ipAddress.set(ip);
        userAgent.set(agent);
    }

    /** Never throws: auditing must not be able to fail a request. */
    public String getIpAddress() {
        return ipAddress.get();
    }

    public String getUserAgent() {
        return userAgent.get();
    }

    public void clear() {
        ipAddress.remove();
        userAgent.remove();
    }
}
