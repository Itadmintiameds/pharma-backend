package tiameds.pharmabackend.entity.product;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_batch_details")
public class BatchDetails {

    @Id
    @Column(name = "batch_id", length = 30)
    private String batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_id", referencedColumnName = "packaging_id")
    @JsonIgnore
    private PackagingDetails packagingDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    @JsonIgnore
    private ProductDetails product;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "purchase_unit", length = 50)
    private String purchaseUnit;

    @Column(name = "stock_quantity")
    private Long stockQuantity;

    @Column(name = "free_unit", length = 50)
    private String freeUnit;

    @Column(name = "free_quantity")
    private Long freeQuantity;

    @Column(name = "purchase_price")
    private Double purchasePrice;

    @Column(name = "mrp")
    private Double mrp;

    @Column(name = "selling_price")
    private Double sellingPrice;

    @Column(name = "purchase_price_per_unit")
    private Double purchasePricePerUnit;

    @Column(name = "mrp_per_unit")
    private Double mrpPerUnit;

    @Column(name = "selling_price_per_unit")
    private Double sellingPricePerUnit;

    @Column(name = "rack_location", length = 100)
    private String rackLocation;

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by", length = 30)
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(
            mappedBy = "batch",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<PurchaseDetails> purchaseDetails = new ArrayList<>();
}
