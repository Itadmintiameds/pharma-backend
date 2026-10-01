package tiameds.pharmabackend.entity.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_warehouse_return_details")
public class WarehouseReturnDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_return_detail_id")
    private Long warehouseReturnDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_return_id", referencedColumnName = "warehouse_return_id")
    @JsonIgnore
    private WarehouseReturn warehouseReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    @JsonIgnore
    private ProductDetails product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", referencedColumnName = "batch_id")
    @JsonIgnore
    private BatchDetails batch;

    @Column(name = "return_quantity")
    private Long returnQuantity;

    @Column(name = "dispatch_quantity")
    private Long dispatchQuantity;

    @Column(name = "received_quantity")
    private Long receivedQuantity;

    @Column(name = "not_received_quantity")
    private Long notReceivedQuantity;

    @Column(name = "return_reason")
    private String returnReason;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
