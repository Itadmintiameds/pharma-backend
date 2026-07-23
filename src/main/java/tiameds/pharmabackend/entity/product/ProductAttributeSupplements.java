package tiameds.pharmabackend.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.master.*;


import java.time.LocalDateTime;

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

    @Column(name = "net_quantity_unit", length = 20)
    private String netQuantityUnit;

    //join master table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_group_id", referencedColumnName = "age_group_id")
    @JsonIgnore
    private AgeGroup ageGroup;

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
