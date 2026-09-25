package tiameds.pharmabackend.entity.purchase;

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
@Table(name = "pharma_purchase_return_details")
public class PurchaseReturnDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_return_detail_id")
    private Long purchaseReturnDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_return_id", referencedColumnName = "purchase_return_id")
    @JsonIgnore
    private PurchaseReturn purchaseReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    @JsonIgnore
    private ProductDetails product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", referencedColumnName = "batch_id")
    @JsonIgnore
    private BatchDetails batch;

    @Column(name = "purchase_return_quantity")
    private Long purchaseReturnQuantity;

    // A count, like purchaseReturnQuantity, and in the same purchase unit —
    // it has to be arithmetic because free units are stock that moves.
    @Column(name = "free_return_quantity")
    private Long freeReturnQuantity;

    @Column(name = "return_reason")
    private String returnReason;

    @Column(name = "gross_amount")
    private BigDecimal grossAmount;

    @Column(name = "gst_amount")
    private BigDecimal gstAmount;

    @Column(name = "net_amount")
    private BigDecimal netAmount;

    // Lines are never edited in place once a return is revised: the old row is
    // marked inactive and a new row carries the next revision number, pointing
    // back at the row it supersedes. Nullable so ddl-auto can add the columns to
    // a table that already has rows — a null reads as revision 1 / active.
    @Column(name = "revision_no")
    private Integer revisionNo = 1;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "previous_detail_id")
    private Long previousDetailId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    // True for the line's latest revision. Rows written before revisioning have
    // a null flag and count as current.
    public boolean isCurrent() {
        return !Boolean.FALSE.equals(isActive);
    }

    public int currentRevisionNo() {
        return revisionNo != null ? revisionNo : 1;
    }
}
