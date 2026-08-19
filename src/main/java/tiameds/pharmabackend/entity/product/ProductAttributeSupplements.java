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
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_product_attribute_supplements")
public class ProductAttributeSupplements {

    @Id
    @Column(name = "product_attribute_id", length = 30)
    private String productAttributeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    @JsonIgnore
    private ProductDetails product;

    // Temporary basic mappings until master entities are created
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapeutic_category_id", referencedColumnName = "therapeutic_category_id")
    @JsonIgnore
    private TherapeuticCategory therapeuticCategory;

    //join master table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapeutic_subcategory_id", referencedColumnName = "therapeutic_subcategory_id")
    @JsonIgnore
    private TherapeuticSubcategory therapeuticSubcategory;


    //join master table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flavour_id", referencedColumnName = "flavour_id")
    @JsonIgnore
    private Flavour flavour;

    //join master table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dosage_form_id", referencedColumnName = "dosage_id")
    @JsonIgnore
    private DosageForm dosageForm;

    @Column(name = "strength_composition", length = 500)
    private String strengthComposition;

    @Column(name = "net_quantity")
    private Double netQuantity;

//    @Column(name = "net_quantity_unit", length = 20)
//    private String netQuantityUnit;

    // FK → Net Quantity Unit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "net_quantity_unit_id", referencedColumnName = "net_quantity_unit_id")
    @JsonIgnore
    private NetQuantityUnit netQuantityUnit;

    //join master table
    //join Age master table
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_supplement_age_group_mapping",
            joinColumns = @JoinColumn(name = "product_attribute_id"),
            inverseJoinColumns = @JoinColumn(name = "age_group_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<AgeGroup> ageGroups = new ArrayList<>();

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "manufacturer_name")
    private String manufacturerName;

    @Column(name = "fssai_license_number", length = 50)
    private String fssaiLicenseNumber;

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by", length = 30)
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
