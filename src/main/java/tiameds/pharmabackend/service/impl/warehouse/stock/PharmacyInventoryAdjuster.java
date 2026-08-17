package tiameds.pharmabackend.service.impl.warehouse.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.entity.purchase.InventoryAudit;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.enums.StockMovement;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.purchase.InventoryAuditRepository;
import tiameds.pharmabackend.repository.purchase.InventoryRepository;
import tiameds.pharmabackend.service.warehouse.stock.InventoryAdjuster;
import tiameds.pharmabackend.service.warehouse.stock.StockAdjustment;

// Adjusts pharmacy stock (pharma_inventory + pharma_inventory_audit).
// Used for the destination leg of warehouse->pharmacy, and both legs of pharmacy->pharmacy.
@Component
@RequiredArgsConstructor
public class PharmacyInventoryAdjuster implements InventoryAdjuster {

    private final PharmacyDetailsRepository pharmacyRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryAuditRepository auditRepository;

    @Override
    public LocationType locationType() {
        return LocationType.PHARMACY;
    }

    @Override
    public void decrement(StockAdjustment adj) {
        Inventory inv = inventoryRepository
                .findByPharmacy_PharmacyIdAndProductAndPackagingAndBatch(
                        adj.locationId(), adj.product(), adj.packaging(), adj.batch())
                .orElseThrow(() -> new IllegalStateException(
                        "No pharmacy stock for this batch in pharmacy " + adj.locationId()));

        long remaining = inv.getTotalStock() - adj.quantity();
        if (remaining < 0) {
            throw new IllegalStateException("Insufficient pharmacy stock: have "
                    + inv.getTotalStock() + ", need " + adj.quantity());
        }

        inv.setTotalStock(remaining);
        inv.setModifiedBy(adj.actor());
        inv.setModifiedAt(adj.timestamp());
        writeAudit(inv, adj, StockMovement.OUT, remaining);
    }

    @Override
    public void increment(StockAdjustment adj) {
        Inventory inv = inventoryRepository
                .findByPharmacy_PharmacyIdAndProductAndPackagingAndBatch(
                        adj.locationId(), adj.product(), adj.packaging(), adj.batch())
                .orElseGet(() -> newRow(adj));

        long remaining = inv.getTotalStock() + adj.quantity();
        inv.setTotalStock(remaining);
        inv.setModifiedBy(adj.actor());
        inv.setModifiedAt(adj.timestamp());
        inventoryRepository.save(inv);          // insert for a new row; managed rows flush on commit
        mapProductToPharmacy(adj);              // stock arriving here also maps the product to this pharmacy
        writeAudit(inv, adj, StockMovement.IN, remaining);
    }

    // Ensures the product is listed under this pharmacy so it shows in the pharmacy's
    // product/stock views (mirrors how onboarding/purchase map products to a location).
    private void mapProductToPharmacy(StockAdjustment adj) {
        ProductDetails product = adj.product();
        boolean mapped = product.getPharmacies().stream()
                .anyMatch(p -> adj.locationId().equals(p.getPharmacyId()));
        if (!mapped) {
            product.getPharmacies().add(pharmacyRepository.getReferenceById(adj.locationId()));
        }
    }

    private Inventory newRow(StockAdjustment adj) {
        Inventory inv = new Inventory();
        inv.setPharmacy(pharmacyRepository.getReferenceById(adj.locationId()));
        inv.setProduct(adj.product());
        inv.setPackaging(adj.packaging());
        inv.setBatch(adj.batch());
        inv.setTotalStock(0L);
        inv.setCreatedBy(adj.actor());
        inv.setCreatedAt(adj.timestamp());
        return inv;
    }

    private void writeAudit(Inventory inv, StockAdjustment adj,
                            StockMovement movement, long remaining) {
        InventoryAudit audit = new InventoryAudit();
        audit.setInventory(inv);
        audit.setPharmacy(inv.getPharmacy());
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
