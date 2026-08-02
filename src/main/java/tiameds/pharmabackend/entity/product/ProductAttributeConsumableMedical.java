package tiameds.pharmabackend.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.master.DeviceCategory;
import tiameds.pharmabackend.entity.master.DeviceSpecificationUnit;
import tiameds.pharmabackend.entity.master.DeviceSubCategory;
import tiameds.pharmabackend.entity.master.MaterialType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_product_attribute_consumable_medical")
public class ProductAttributeConsumableMedical {

    @Id
    @Column(name = "product_attribute_id", length = 30)
    private String productAttributeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    @JsonIgnore
    private ProductDetails product;

    // FK → tbl_device_category_master
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_category_id")
    @JsonIgnoreProperties
    private DeviceCategory deviceCategory;

    // FK → tbl_device_category_master
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_sub_category_id")
    @JsonIgnoreProperties
    private DeviceSubCategory deviceSubCategory;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_consumable_material_mapping",
            joinColumns = @JoinColumn(name = "product_attribute_id"),
            inverseJoinColumns = @JoinColumn(name = "material_type_id")
    )
    @JsonIgnore
    private List<MaterialType> materialTypes;

    @Column(name = "dimension_Size")
    private String dimensionSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_specification_unit_id")
    @JsonIgnore
    private DeviceSpecificationUnit deviceSpecificationUnit;

    @Column(name = "sterile_or_non_sterile")
    private String sterileOrNonSterile;

    @Column(name = "disposal_or_non_disposal")
    private String disposalOrNonDisposal;

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;     // Intended Use / Purpose*

    @Column(name = "manufacturer_name", length = 100)
    private String manufacturerName;        // Manufacturer Name*

    @Column(name = "manufacturer_license_number", length = 100)
    private String manufacturerLicenseNumber;        // Manufacturer License Number*

    @Column(name = "is_iso_certified")
    private Boolean isISOCertified;

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by", length = 30)
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
