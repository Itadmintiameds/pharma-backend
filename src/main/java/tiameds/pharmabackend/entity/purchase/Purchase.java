package tiameds.pharmabackend.entity.purchase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;
import tiameds.pharmabackend.entity.supplier.SupplierPayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_purchase")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_id")
    private Long purchaseId;

    @Column(name = "pharmacy_id")
    private String pharmacyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id")
    @JsonIgnore
    private SupplierMaster supplier;

    @Column(name = "grn_no")
    private String grnNo;

    @Column(name = "invoice_no")
    private String invoiceNo;

    @Column(name = "invoice_date")
    private LocalDateTime invoiceDate;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "credit_days")
    private Long creditDays;

    @Column(name = "supplier_payment_status")
    private String supplierPaymentStatus;

    @Column(name = "total_gross_amount")
    private BigDecimal totalGrossAmount;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @Column(name = "total_gst")
    private BigDecimal totalGst;

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
            mappedBy = "purchase",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<PurchaseDetails> purchaseDetails = new ArrayList<>();

    @OneToMany(
            mappedBy = "purchase",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonIgnore
    private List<SupplierPayment> supplierPayments = new ArrayList<>();
}
