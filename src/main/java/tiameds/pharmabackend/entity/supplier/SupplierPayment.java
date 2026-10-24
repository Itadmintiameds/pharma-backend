package tiameds.pharmabackend.entity.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.purchase.Purchase;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_supplier_payment")
public class SupplierPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_payment_id")
    private Long supplierPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonIgnore
    private SupplierMaster supplierMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    @JsonIgnore
    private Purchase purchase;

    @Column(name = "supplier_payment_no")
    private String supplierPaymentNo;

    @Column(name = "supplier_payment_date")
    private LocalDate supplierPaymentDate;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "payment_amount")
    private Double paymentAmount;

    @Column(name = "transaction_no")
    private String transactionNo;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private String modifiedAt;
}
