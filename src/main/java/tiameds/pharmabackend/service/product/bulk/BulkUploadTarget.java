package tiameds.pharmabackend.service.product.bulk;

import tiameds.pharmabackend.enums.LocationType;

/**
 * Where a bulk upload lands: the pharmacy or warehouse the caller is operating on,
 * resolved once from the request and reused for every row.
 */
public record BulkUploadTarget(LocationType type, String locationId, String locationName) {

    public boolean isWarehouse() {
        return type == LocationType.WAREHOUSE;
    }
}
