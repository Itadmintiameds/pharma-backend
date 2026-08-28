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
import tiameds.pharmabackend.context.CurrentWarehouseContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CurrentPharmacyFilter extends OncePerRequestFilter {

    private final PharmacyDetailsRepository pharmacyRepository;

    private final UserDetailsRepository userDetailsRepository;

    private final CurrentPharmacyContext pharmacyContext;

    private final CurrentWarehouseContext warehouseContext;

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

                // Warehouse managers operate on a warehouse (selected via X-Warehouse-Id),
                // everyone else on a pharmacy (selected via X-Pharmacy-Id).
                if (locationContextResolver.isWarehouseManager(currentUser.getUser())) {

                    String warehouseId = request.getHeader("X-Warehouse-Id");

                    System.out.println("Header Warehouse : " + warehouseId);

                    if (warehouseId != null && !warehouseId.isBlank()) {

                        boolean valid =
                                userDetailsRepository.existsUserWarehouse(
                                        warehouseId,
                                        currentUser.getUserId());

                        if (!valid) {

                            response.sendError(
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "Invalid Warehouse");

                            return;
                        }

                        warehouseContext.setCurrentWarehouse(warehouseId);
                        System.out.println("Logged in User : " + currentUser.getUserId());
                    }
                } else if (locationContextResolver.isSuperAdmin(currentUser.getUser())) {

                    // SUPER ADMIN: operate on a warehouse in their own organization
                    // when X-Warehouse-Id is sent (authorized by same-organization,
                    // not by an explicit user<->warehouse mapping); otherwise fall
                    // back to a pharmacy they belong to via X-Pharmacy-Id (same rule
                    // as everyone else).
                    String warehouseId = request.getHeader("X-Warehouse-Id");

                    System.out.println("Header Warehouse (super admin) : " + warehouseId);

                    if (warehouseId != null && !warehouseId.isBlank()) {

                        boolean valid =
                                locationContextResolver.warehouseInUserOrganization(
                                        warehouseId,
                                        currentUser.getUser());

                        if (!valid) {

                            response.sendError(
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "Invalid Warehouse");

                            return;
                        }

                        warehouseContext.setCurrentWarehouse(warehouseId);
                        System.out.println("Logged in User : " + currentUser.getUserId());

                    } else {

                        String pharmacyId = request.getHeader("X-Pharmacy-Id");

                        System.out.println("Header Pharmacy (super admin) : " + pharmacyId);

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
                } else {

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
            warehouseContext.clear();
            System.out.println("Context Cleared");
        }
    }
}