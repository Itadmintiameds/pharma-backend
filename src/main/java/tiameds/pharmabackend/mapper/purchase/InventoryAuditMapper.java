package tiameds.pharmabackend.mapper.purchase;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.purchase.InventoryAuditDto;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.entity.purchase.InventoryAudit;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;

@Component
public class InventoryAuditMapper {

    public InventoryAuditDto toDto(InventoryAudit audit) {

        InventoryAuditDto dto = new InventoryAuditDto();

        dto.setInventoryAuditId(audit.getInventoryAuditId());
        dto.setInventoryId(
                audit.getInventory() != null
                        ? audit.getInventory().getInventoryId()
                        : null
        );
        dto.setPurchaseDetailsId(
                audit.getPurchaseDetails() != null
                        ? audit.getPurchaseDetails().getPurchaseDetailsId()
                        : null
        );
        dto.setBillingId(
                audit.getBilling() != null
                        ? audit.getBilling().getBillingId()
                        : null
        );
        dto.setStockMovement(audit.getStockMovement());
        dto.setTransactionType(audit.getTransactionType());
        dto.setChangeStock(audit.getChangeStock());
        dto.setRemainingStock(audit.getRemainingStock());
        dto.setChangedBy(audit.getChangedBy());
        dto.setChangedAt(audit.getChangedAt());

        return dto;
    }

    public InventoryAudit toEntity(
            InventoryAuditDto dto,
            Inventory inventory,
            PurchaseDetails purchaseDetails
    ) {
        return toEntity(dto, inventory, purchaseDetails, null);
    }

    public InventoryAudit toEntity(
            InventoryAuditDto dto,
            Inventory inventory,
            PurchaseDetails purchaseDetails,
            Billing billing
    ) {

        InventoryAudit audit = new InventoryAudit();

        audit.setInventoryAuditId(dto.getInventoryAuditId());
        audit.setInventory(inventory);
        audit.setPurchaseDetails(purchaseDetails);
        audit.setBilling(billing);
        audit.setStockMovement(dto.getStockMovement());
        audit.setTransactionType(dto.getTransactionType());
        audit.setChangeStock(dto.getChangeStock());
        audit.setRemainingStock(dto.getRemainingStock());
        audit.setChangedBy(dto.getChangedBy());
        audit.setChangedAt(dto.getChangedAt());

        return audit;
    }
}