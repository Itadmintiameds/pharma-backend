package tiameds.pharmabackend.context;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.repository.UserDetailsRepository;

import java.util.Optional;

/**
 * Single place that decides whether a request operates on a warehouse or a pharmacy.
 *
 * <p>This is driven by the user's role, not sprinkled as if/else across every API:
 * a WAREHOUSE MANAGER resolves to their single bound warehouse, everyone else resolves
 * to the pharmacy chosen via the X-Pharmacy-Id header (see {@link CurrentPharmacyContext}).
 */
@Component
@RequiredArgsConstructor
public class LocationContextResolver {

    // Exact role_name stored for the warehouse-manager role (role_id = 4).
    private static final String WAREHOUSE_MANAGER_ROLE = "WAREHOUSE MANAGER";

    private final CurrentPharmacyContext pharmacyContext;
    private final UserDetailsRepository userDetailsRepository;

    /**
     * Resolves the location the given user is operating on.
     * The user's role is read from the (eagerly-loaded) role association, so this is
     * safe to call with the detached user from the security context.
     */
    public LocationContext resolve(UserDetails user) {

        if (isWarehouseManager(user)) {

            // Fetched via query rather than lazy navigation so it works on a
            // detached user (the warehouse association on UserDetails is LAZY).
            String warehouseId = userDetailsRepository
                    .findWarehouseIdByUserId(user.getUserId())
                    .orElseThrow(() -> new RuntimeException(
                            "Warehouse manager is not assigned to any warehouse : "
                                    + user.getUserId()));

            return new LocationContext(LocationType.WAREHOUSE, warehouseId);
        }

        return new LocationContext(
                LocationType.PHARMACY,
                pharmacyContext.getCurrentPharmacy());
    }

    /** The warehouse this user manages, or empty if they are not a warehouse manager. */
    public Optional<String> managedWarehouseId(UserDetails user) {
        if (!isWarehouseManager(user)) {
            return Optional.empty();
        }
        return userDetailsRepository.findWarehouseIdByUserId(user.getUserId());
    }

    public boolean isWarehouseManager(UserDetails user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() != null
                && WAREHOUSE_MANAGER_ROLE.equalsIgnoreCase(
                        user.getRole().getRoleName().trim());
    }
}
