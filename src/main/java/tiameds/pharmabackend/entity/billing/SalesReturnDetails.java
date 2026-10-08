package tiameds.pharmabackend.entity.billing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_sales_return_details")
public class SalesReturnDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_return_detail_id")
    private Long salesReturnDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_return_id", referencedColumnName = "sales_return_id")
    @JsonIgnore
    private SalesReturn salesReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    @JsonIgnore
    private ProductDetails product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", referencedColumnName = "batch_id")
    @JsonIgnore
    private BatchDetails batch;

    @Column(name = "sales_return_quantity")
    private Long salesReturnQuantity;

    @Column(name = "sales_return_reason")
    private String salesReturnReason;

    @Column(name = "gross_amount")
    private BigDecimal grossAmount;

    @Column(name = "gst_amount")
    private BigDecimal gstAmount;

    @Column(name = "net_amount")
    private BigDecimal netAmount;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
