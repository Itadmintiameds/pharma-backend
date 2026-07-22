package tiameds.pharmabackend.entity.product;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_batch_details")
public class PharmaBatchDetails {

    @Id
    @Column(name = "batch_id", length = 30)
    private String batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    @JsonIgnore
    private PharmaProductDetails product;

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
}
