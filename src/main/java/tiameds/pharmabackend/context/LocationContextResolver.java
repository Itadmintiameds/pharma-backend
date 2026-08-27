package tiameds.pharmabackend.context;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;

import java.util.List;

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

    // Exact role_name stored for the super-admin role (matches AccessChecker).
    private static final String SUPER_ADMIN_ROLE = "SUPER ADMIN";

    private final CurrentPharmacyContext pharmacyContext;
    private final CurrentWarehouseContext warehouseContext;
    private final UserDetailsRepository userDetailsRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Resolves the location the given user is operating on.
     * The user's role is read from the (eagerly-loaded) role association, so this is
     * safe to call with the detached user from the security context.
     *
     * <p>A warehouse manager may be mapped to several warehouses; the one for this
     * request comes from the {@code X-Warehouse-Id} header ({@link CurrentWarehouseContext}).
     * If they are mapped to exactly one warehouse the header is optional.
     */
    public LocationContext resolve(UserDetails user) {

        if (isWarehouseManager(user)) {
            return new LocationContext(LocationType.WAREHOUSE, resolveWarehouseId(user));
        }

        // A SUPER ADMIN may operate on a warehouse in their own organization. When the
        // X-Warehouse-Id header was sent and validated (same-organization) by
        // CurrentPharmacyFilter, the warehouse context is set and we route there,
        // making the superadmin behave like a warehouse manager for that warehouse.
        // With no warehouse selected they fall through to the pharmacy branch below.
        if (isSuperAdmin(user)) {
            String warehouseId = warehouseContext.getCurrentWarehouseOrNull();
            if (warehouseId != null && !warehouseId.isBlank()) {
                return new LocationContext(LocationType.WAREHOUSE, warehouseId);
            }
        }

        return new LocationContext(
                LocationType.PHARMACY,
                pharmacyContext.getCurrentPharmacy());
    }

    private String resolveWarehouseId(UserDetails user) {

        // Fetched via query rather than lazy navigation so it works on a detached
        // user (the warehouses association on UserDetails is LAZY).
        List<String> warehouseIds = userDetailsRepository
                .findWarehouseIdsByUserId(user.getUserId());

        if (warehouseIds.isEmpty()) {
            throw new RuntimeException(
                    "Warehouse manager is not mapped to any warehouse : " + user.getUserId());
        }

        String selected = warehouseContext.getCurrentWarehouseOrNull();

        if (selected != null && !selected.isBlank()) {
            if (!warehouseIds.contains(selected)) {
                throw new RuntimeException(
                        "You are not mapped to warehouse : " + selected);
            }
            return selected;
        }

        // No explicit selection: fine when there is exactly one warehouse,
        // otherwise the caller must pick via the X-Warehouse-Id header.
        if (warehouseIds.size() == 1) {
            return warehouseIds.get(0);
        }

        throw new RuntimeException(
                "You are mapped to multiple warehouses; select one via the X-Warehouse-Id header");
    }

    // OLD: single warehouse per user.
    // public Optional<String> managedWarehouseId(UserDetails user) {
    //     if (!isWarehouseManager(user)) {
    //         return Optional.empty();
    //     }
    //     return userDetailsRepository.findWarehouseIdByUserId(user.getUserId());
    // }

    /** All warehouses this user manages; empty if they are not a warehouse manager. */
    public List<String> managedWarehouseIds(UserDetails user) {
        if (!isWarehouseManager(user)) {
            return List.of();
        }
        return userDetailsRepository.findWarehouseIdsByUserId(user.getUserId());
    }

    public boolean isWarehouseManager(UserDetails user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() != null
                && WAREHOUSE_MANAGER_ROLE.equalsIgnoreCase(
                        user.getRole().getRoleName().trim());
    }

    public boolean isSuperAdmin(UserDetails user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() != null
                && SUPER_ADMIN_ROLE.equalsIgnoreCase(
                        user.getRole().getRoleName().trim());
    }

    /**
     * True when the warehouse belongs to the same organization as the user.
     * This is the authorization rule for a SUPER ADMIN operating on a warehouse:
     * unlike a warehouse manager (checked via the user&lt;-&gt;warehouse mapping),
     * a superadmin only needs to be in the warehouse's organization.
     */
    public boolean warehouseInUserOrganization(String warehouseId, UserDetails user) {
        if (warehouseId == null || warehouseId.isBlank() || user == null) {
            return false;
        }
        return warehouseRepository.existsWarehouseInUserOrganization(
                warehouseId, user.getUserId());
    }
}
