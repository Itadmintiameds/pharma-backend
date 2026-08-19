package tiameds.pharmabackend.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.enums.LocationType;

/**
 * The location (warehouse vs pharmacy) a user is operating on for the current request.
 * Warehouse managers act on their single bound warehouse; everyone else acts on the
 * pharmacy selected via the X-Pharmacy-Id header.
 */
@Getter
@RequiredArgsConstructor
public class LocationContext {

    private final LocationType type;
    private final String locationId;

    public boolean isWarehouse() {
        return type == LocationType.WAREHOUSE;
    }

    public boolean isPharmacy() {
        return type == LocationType.PHARMACY;
    }
}
