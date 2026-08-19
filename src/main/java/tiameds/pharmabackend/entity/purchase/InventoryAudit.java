package tiameds.pharmabackend.entity.purchase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.warehouse.WarehouseDistributionDetails;
import tiameds.pharmabackend.enums.StockMovement;
import tiameds.pharmabackend.enums.TransactionType;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_inventory_audit")
public class InventoryAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_audit_id")
    private Long inventoryAuditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    @JsonIgnore
    private Inventory inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", referencedColumnName = "pharmacy_id")
    @JsonIgnore
    private PharmacyDetails pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_details_id")
    @JsonIgnore
    private PurchaseDetails purchaseDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", referencedColumnName = "billing_id")
    @JsonIgnore
    private Billing billing;

    // Set on stock-transfer audit rows (e.g. pharmacy -> pharmacy) to trace back to the allocation that moved the stock
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_distribution_details_id")
    @JsonIgnore
    private WarehouseDistributionDetails warehouseDistributionDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_movement")
    private StockMovement stockMovement;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    @Column(name = "change_stock")
    private Long changeStock;

    @Column(name = "remaining_stock")
    private Long remainingStock;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;
}
