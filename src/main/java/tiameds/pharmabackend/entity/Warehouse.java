package tiameds.pharmabackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.product.ProductDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pharma_warehouse")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "warehouse_code")
    private String warehouseCode;   // Optional

    @Column(name = "warehouse_address")
    private String warehouseAddress;

    @Column(name = "contact_person_name")
    private String contactPersonName;   // Optional

    @Column(name = "mobile_number")
    private String mobileNumber;   // Optional

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    // A warehouse belongs to one organization (many warehouses per organization)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "organization_id")
    @JsonIgnore
    private PharmacyOrganization organization;

    // Warehouse <-> Product ManyToMany (inverse side; ProductDetails owns the join table)
    @ManyToMany(mappedBy = "warehouses", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductDetails> products = new ArrayList<>();
}
