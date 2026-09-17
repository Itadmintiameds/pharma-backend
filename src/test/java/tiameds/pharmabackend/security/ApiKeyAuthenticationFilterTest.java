package tiameds.pharmabackend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The filter is silent by design — every rejection falls through to a bare 401 — so
 * its path matching is worth pinning down here rather than in a running container.
 */
class ApiKeyAuthenticationFilterTest {

    private static final String KEY = "34a70d64cc79bd35dc59a487b089de21b0abec4e6d0aa19a6d72ca3019f2be6c";
    private static final String CONTEXT_PATH = "/api/v1";

    private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter();

    ApiKeyAuthenticationFilterTest() {
        ReflectionTestUtils.setField(filter, "configuredApiKey", KEY);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds a request the way Tomcat presents one behind a context path, deliberately
     * leaving servletPath empty — the value this filter must no longer depend on.
     */
    private MockHttpServletRequest request(String method, String pathWithinApp) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, CONTEXT_PATH + pathWithinApp);
        request.setContextPath(CONTEXT_PATH);
        request.setServletPath("");
        return request;
    }

    private void invoke(MockHttpServletRequest request) throws Exception {
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void authenticatesBulkUploadWithAValidKey() throws Exception {
        MockHttpServletRequest request = request("POST", "/product/bulk-upload");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, KEY);

        invoke(request);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "a valid key on an allowlisted path must authenticate");
        assertEquals("admin-service", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void authenticatesTheTemplateEndpointToo() throws Exception {
        MockHttpServletRequest request = request("GET", "/product/bulk-upload/template");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, KEY);

        invoke(request);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void ignoresTheQueryString() throws Exception {
        MockHttpServletRequest request = request("POST", "/product/bulk-upload");
        request.setQueryString("pharmacyId=PRPHA0001&dryRun=true");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, KEY);

        invoke(request);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void stillMatchesWhenThereIsNoContextPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/product/bulk-upload");
        request.setContextPath("");
        request.setServletPath("");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, KEY);

        invoke(request);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsAWrongKey() throws Exception {
        MockHttpServletRequest request = request("POST", "/product/bulk-upload");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "not-the-key");

        invoke(request);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doesNotAuthenticateAPathOutsideTheAllowlist() throws Exception {
        MockHttpServletRequest request = request("GET", "/product/stock-summary");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, KEY);

        invoke(request);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "the key must not unlock endpoints it was not granted");
    }

    @Test
    void doesNothingWithoutTheHeader() throws Exception {
        invoke(request("POST", "/product/bulk-upload"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
