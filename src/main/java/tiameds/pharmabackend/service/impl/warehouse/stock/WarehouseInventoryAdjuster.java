package tiameds.pharmabackend.service.impl.warehouse.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.warehouse.WarehouseInventory;
import tiameds.pharmabackend.entity.warehouse.WarehouseInventoryAudit;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.enums.StockMovement;
import tiameds.pharmabackend.repository.warehouse.WarehouseInventoryAuditRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseInventoryRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.warehouse.stock.InventoryAdjuster;
import tiameds.pharmabackend.service.warehouse.stock.StockAdjustment;

// Adjusts warehouse stock (pharma_warehouse_inventory + pharma_warehouse_inventory_audit).
@Component
@RequiredArgsConstructor
public class WarehouseInventoryAdjuster implements InventoryAdjuster {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryRepository inventoryRepository;
    private final WarehouseInventoryAuditRepository auditRepository;

    @Override
    public LocationType locationType() {
        return LocationType.WAREHOUSE;
    }

    @Override
    public void decrement(StockAdjustment adj) {
        WarehouseInventory inv = inventoryRepository
                .findByWarehouse_WarehouseIdAndProductAndPackagingAndBatch(
                        adj.locationId(), adj.product(), adj.packaging(), adj.batch())
                .orElseThrow(() -> new IllegalStateException(
                        "No warehouse stock for this batch in warehouse " + adj.locationId()));

        long remaining = inv.getTotalStock() - adj.quantity();
        if (remaining < 0) {
            throw new IllegalStateException("Insufficient warehouse stock: have "
                    + inv.getTotalStock() + ", need " + adj.quantity());
        }

        inv.setTotalStock(remaining);
        inv.setModifiedBy(adj.actor());
        inv.setModifiedAt(adj.timestamp());
        writeAudit(inv, adj, StockMovement.OUT, remaining);
    }

    @Override
    public void increment(StockAdjustment adj) {
        WarehouseInventory inv = inventoryRepository
                .findByWarehouse_WarehouseIdAndProductAndPackagingAndBatch(
                        adj.locationId(), adj.product(), adj.packaging(), adj.batch())
                .orElseGet(() -> newRow(adj));

        long remaining = inv.getTotalStock() + adj.quantity();
        inv.setTotalStock(remaining);
        inv.setModifiedBy(adj.actor());
        inv.setModifiedAt(adj.timestamp());
        inventoryRepository.save(inv);          // insert for a new row; managed rows flush on commit
        mapProductToWarehouse(adj);             // stock arriving here also maps the product to this warehouse
        writeAudit(inv, adj, StockMovement.IN, remaining);
    }

    // Ensures the product is listed under this warehouse so it shows in the warehouse's
    // product/stock views (mirrors how onboarding/purchase map products to a location).
    private void mapProductToWarehouse(StockAdjustment adj) {
        ProductDetails product = adj.product();
        boolean mapped = product.getWarehouses().stream()
                .anyMatch(w -> adj.locationId().equals(w.getWarehouseId()));
        if (!mapped) {
            product.getWarehouses().add(warehouseRepository.getReferenceById(adj.locationId()));
        }
    }

    private WarehouseInventory newRow(StockAdjustment adj) {
        WarehouseInventory inv = new WarehouseInventory();
        inv.setWarehouse(warehouseRepository.getReferenceById(adj.locationId()));
        inv.setProduct(adj.product());
        inv.setPackaging(adj.packaging());
        inv.setBatch(adj.batch());
        inv.setTotalStock(0L);
        inv.setCreatedBy(adj.actor());
        inv.setCreatedAt(adj.timestamp());
        return inv;
    }

    private void writeAudit(WarehouseInventory inv, StockAdjustment adj,
                            StockMovement movement, long remaining) {
        WarehouseInventoryAudit audit = new WarehouseInventoryAudit();
        audit.setWarehouseInventory(inv);
        audit.setWarehouse(inv.getWarehouse());
        audit.setWarehouseDistributionDetails(adj.distributionDetails());
        audit.setPurchaseDetails(adj.purchaseDetails());
        audit.setStockMovement(movement);
        audit.setTransactionType(adj.transactionType());
        audit.setChangeStock(adj.quantity());
        audit.setRemainingStock(remaining);
        audit.setChangedBy(adj.actor());
        audit.setChangedAt(adj.timestamp());
        auditRepository.save(audit);
    }
}
