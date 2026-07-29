package tiameds.pharmabackend.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.master.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_product_attribute_non_consumable_medical")
public class ProductAttributeNonConsumableMedical {

    @Id
    @Column(name = "product_attribute_id")
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

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "device_classification", length = 100)
    private String deviceClassification;    // Device Classification (Class A/B/C/D)*

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;     // Intended Use / Purpose*

    @Column(name = "dimension_Size")
    private String dimensionSize;       // Technical Dimensions / Capacity / Configuration*

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_non_consumable_material_mapping",
            joinColumns = @JoinColumn(name = "product_attribute_id"),
            inverseJoinColumns = @JoinColumn(name = "material_type_id")
    )
    @JsonIgnore
    private List<MaterialType> materialTypes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "power_source_id")
    @JsonIgnore
    private PowerSource powerSource;

    @Column(name = "warranty_period")
    private String warrantyPeriod;      // Only number allowed. • Max 3 chars.

    @Column(name = "service_availability")
    private Boolean serviceAvailability = true;        // AMC / Service Availability* (Yes/No)

    @Column(name = "manufacturer_name", nullable = false, length = 100)
    private String manufacturerName;        // Manufacturer Name*

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    @JsonIgnore
    private Country countryMaster;     // Country of Origin*

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by", length = 30)
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
