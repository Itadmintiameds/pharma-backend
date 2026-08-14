package tiameds.pharmabackend.entity.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.enums.StockMovement;
import tiameds.pharmabackend.enums.TransactionType;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pharma_warehouse_inventory_audit")
public class WarehouseInventoryAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_inventory_audit_id")
    private Long warehouseInventoryAuditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_inventory_id", referencedColumnName = "warehouse_inventory_id")
    @JsonIgnore
    private WarehouseInventory warehouseInventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", referencedColumnName = "warehouse_id")
    @JsonIgnore
    private Warehouse warehouse;

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
