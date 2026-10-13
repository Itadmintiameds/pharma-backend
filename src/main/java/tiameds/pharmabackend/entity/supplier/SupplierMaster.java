package tiameds.pharmabackend.entity.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.enums.SupplierStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_supplier_master")
public class SupplierMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "pharmacy_id")
    private String pharmacyId;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "dlno")
    private String dlno;

    @Column(name = "gstin_no")
    private String gstinNo;

    @Column(name = "pan_no")
    private String panNo;

    @Column(name = "dl_expiry_date")
    private LocalDateTime dlExpiryDate;

    @Column(name = "issuing_authority")
    private String issuingAuthority;

    @Column(name = "fssai_no")
    private String fssaiNo;

    @Column(name = "contact_person_name")
    private String contactPersonName;

    @Column(name = "mobile_number")
    private Long mobileNumber;

    @Column(name = "supplier_email")
    private String supplierEmail;

    @Column(name = "address")
    private String address;

    @Column(name = "building_no")
    private String buildingNo;

    @Column(name = "pincode")
    private Long pincode;

    @Column(name = "city")
    private String city;

    @Column(name = "district")
    private String district;

    @Column(name = "state")
    private String state;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_holder_name")
    private String accountHolderName;

    @Column(name = "account_number")
    private Long accountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 20)
    private SupplierStatus status;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(
            mappedBy = "supplier",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<Purchase> purchases = new ArrayList<>();

}
