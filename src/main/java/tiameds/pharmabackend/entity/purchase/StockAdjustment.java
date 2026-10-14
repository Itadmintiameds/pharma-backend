package tiameds.pharmabackend.entity.purchase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_stock_adjustment")
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_adjustment_id")
    private Long stockAdjustmentId;

    @Column(name = "pharmacy_id")
    private String pharmacyId;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    @JsonIgnore
    private ProductDetails product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", referencedColumnName = "batch_id")
    @JsonIgnore
    private BatchDetails batch;

    @Column(name = "stock_adjustment_no")
    private String stockAdjustmentNo;

    @Column(name = "stock_adjustment_date")
    private LocalDateTime stockAdjustmentDate;

    @Column(name = "actual_stock_qty")
    private Long actualStockQty;

    @Column(name = "adjustment_qty")
    private Long adjustmentQty;

    @Column(name = "adjustment_type")
    private String adjustmentType;

    @Column(name = "adjustment_value")
    private Long adjustmentValue;

    @Column(name = "stock_adjustment_reason")
    private String stockAdjustmentReason;

    @Column(name = "stock_adjustment_remarks")
    private String stockAdjustmentRemarks;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
