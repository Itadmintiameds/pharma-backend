package tiameds.pharmabackend.entity.billing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.enums.CustomerType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_billing")
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_id")
    private Long billingId;

    @Column(name = "bill_no")
    private String billNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", referencedColumnName = "pharmacy_id")
    @JsonIgnore
    private PharmacyDetails pharmacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id")
    @JsonIgnore
    private CustomerManagement customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", referencedColumnName = "doctor_id")
    @JsonIgnore
    private DoctorDetails doctor;

    @Column(name = "op_ip_number")
    private String opIpNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type")
    private CustomerType customerType;

    @Column(name = "prescription_url")
    private String prescriptionUrl;

    @Column(name = "total_discount_percentage", precision = 5, scale = 2)
    private BigDecimal totalDiscountPercentage;

    @Column(name = "total_discount_amount")
    private BigDecimal totalDiscountAmount;

    @Column(name = "total_gst_amount")
    private BigDecimal totalGstAmount;

    @Column(name = "total_gross_amount")
    private BigDecimal totalGrossAmount;

    @Column(name = "total_net_amount")
    private BigDecimal totalNetAmount;

    @Column(name = "selling_type")
    private String sellingType;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(
            mappedBy = "billing",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<BillingDetails> billingDetails = new ArrayList<>();

    @OneToMany(
            mappedBy = "billing",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<BillingPayment> billingPayments = new ArrayList<>();
}
