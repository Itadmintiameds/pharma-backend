package tiameds.pharmabackend.enums;

// Source/destination of a stock movement. Backs the sourceType/destinationType
// columns on WarehouseDistribution and selects the matching InventoryAdjuster.
public enum LocationType {
    WAREHOUSE,
    PHARMACY
}
