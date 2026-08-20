package tiameds.pharmabackend.entity.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pharma_warehouse_distribution_details")
public class WarehouseDistributionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_distribution_details_id")
    private Long warehouseDistributionDetailsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_distribution_id", nullable = false)
    @JsonIgnore
    private WarehouseDistribution warehouseDistribution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false)
    @JsonIgnore
    private ProductDetails product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_id")
    @JsonIgnore
    private PackagingDetails packaging;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", referencedColumnName = "batch_id")
    @JsonIgnore
    private BatchDetails batch;

    @Column(name = "issue_quantity", nullable = false)
    private Long issueQuantity;

    @Column(name = "dispatched_quantity")   // set at Dispatch; may be < issueQuantity when the source ships short
    private Long dispatchedQuantity;

    @Column(name = "dispatch_remarks")      // free-text note from the sender at Dispatch (e.g. reason for shipping short)
    private String dispatchRemarks;

    @Column(name = "received_quantity")     // set at Stock Received; may be < dispatchedQuantity on partial receipt / rejection
    private Long receivedQuantity;

    @Column(name = "damaged_quantity")      // reported by the receiver at Stock Received: damaged / not-received units; 0 when all good
    private Long damagedQuantity;

    // OLD: single ambiguous "remarks" — split into dispatchRemarks (sender) and
    // receiveRemarks (receiver) now that both stages capture a note.
    // @Column(name = "remarks")
    // private String remarks;
    @Column(name = "receive_remarks")       // free-text note from the receiver (e.g. reason for damage / shortfall)
    private String receiveRemarks;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
