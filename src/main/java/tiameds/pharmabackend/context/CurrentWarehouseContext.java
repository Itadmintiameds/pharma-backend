package tiameds.pharmabackend.context;

import org.springframework.stereotype.Component;

/**
 * The warehouse a warehouse-manager selected for the current request, taken from
 * the {@code X-Warehouse-Id} header (see {@code CurrentPharmacyFilter}).
 *
 * <p>Unlike {@link CurrentPharmacyContext}, the getter returns {@code null} rather
 * than throwing when nothing is selected: a user mapped to exactly one warehouse
 * does not need to send the header, and {@link LocationContextResolver} applies
 * that fallback.
 */
@Component
public class CurrentWarehouseContext {

    private final ThreadLocal<String> currentWarehouse = new ThreadLocal<>();

    public void setCurrentWarehouse(String warehouseId) {
        currentWarehouse.set(warehouseId);
    }

    /** The selected warehouse id, or {@code null} if none was selected. */
    public String getCurrentWarehouseOrNull() {
        return currentWarehouse.get();
    }

    public void clear() {
        currentWarehouse.remove();
    }
}
