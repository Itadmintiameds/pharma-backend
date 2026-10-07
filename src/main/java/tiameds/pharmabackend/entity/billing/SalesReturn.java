package tiameds.pharmabackend.entity.billing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.enums.SalesReturnStatusConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_sales_return")
public class SalesReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_return_id")
    private Long salesReturnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", referencedColumnName = "billing_id")
    @JsonIgnore
    private Billing billing;

    // Copied from the bill so the return-number sequence and listings can be
    // scoped per pharmacy without joining through the bill.
    @Column(name = "pharmacy_id")
    private String pharmacyId;

    @Column(name = "sales_return_no")
    private String salesReturnNo;

    @Column(name = "sales_return_date")
    private LocalDateTime salesReturnDate;

    // Persisted as its label (Completed).
    @Convert(converter = SalesReturnStatusConverter.class)
    @Column(name = "sales_return_status", length = 20)
    private SalesReturnStatus salesReturnStatus = SalesReturnStatus.COMPLETED;

    @Column(name = "total_gross_amount")
    private BigDecimal totalGrossAmount;

    @Column(name = "total_gst_amount")
    private BigDecimal totalGstAmount;

    @Column(name = "total_net_amount")
    private BigDecimal totalNetAmount;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(
            mappedBy = "salesReturn",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<SalesReturnDetails> salesReturnDetails = new ArrayList<>();
}
