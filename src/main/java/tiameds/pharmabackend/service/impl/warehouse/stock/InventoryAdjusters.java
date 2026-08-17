package tiameds.pharmabackend.service.impl.warehouse.stock;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.service.warehouse.stock.InventoryAdjuster;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves the right {@link InventoryAdjuster} for a location type. Spring injects
 * every adjuster bean; the distribution service picks one per leg from the
 * source/destination type, so warehouse->pharmacy, pharmacy->pharmacy and (later)
 * warehouse->warehouse all work with no branching in the service.
 */
@Component
public class InventoryAdjusters {

    private final Map<LocationType, InventoryAdjuster> byType;

    public InventoryAdjusters(List<InventoryAdjuster> adjusters) {
        this.byType = adjusters.stream()
                .collect(Collectors.toMap(InventoryAdjuster::locationType, a -> a));
    }

    public InventoryAdjuster of(LocationType type) {
        InventoryAdjuster adjuster = byType.get(type);
        if (adjuster == null) {
            throw new IllegalStateException("No InventoryAdjuster registered for " + type);
        }
        return adjuster;
    }
}
