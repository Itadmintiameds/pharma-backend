package tiameds.pharmabackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CurrentPharmacyFilter extends OncePerRequestFilter {

    private final PharmacyDetailsRepository pharmacyRepository;

    private final CurrentPharmacyContext pharmacyContext;

    private final LocationContextResolver locationContextResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            System.out.println("===== CurrentPharmacyFilter =====");
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof UsernamePasswordAuthenticationToken
                    && authentication.getPrincipal() instanceof CustomUserDetails currentUser) {

                // Warehouse managers operate on their bound warehouse, not a pharmacy.
                // Any X-Pharmacy-Id they send is irrelevant, so skip the pharmacy check
                // instead of 403-ing them on every request.
                if (!locationContextResolver.isWarehouseManager(currentUser.getUser())) {

                    String pharmacyId =
                            request.getHeader("X-Pharmacy-Id");

                    System.out.println("Header Pharmacy : " + pharmacyId);

                    if (pharmacyId != null && !pharmacyId.isBlank()) {

                        boolean valid =
                                pharmacyRepository.existsUserPharmacy(
                                        pharmacyId,
                                        currentUser.getUserId());

                        if (!valid) {

                            response.sendError(
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "Invalid Pharmacy");

                            return;
                        }

                        pharmacyContext.setCurrentPharmacy(pharmacyId);
                        System.out.println("Logged in User : " + currentUser.getUserId());
                    }
                }
            }

            filterChain.doFilter(request, response);
            System.out.println("Validation Passed");

        } finally {

            pharmacyContext.clear();
            System.out.println("Context Cleared");
        }
    }
}