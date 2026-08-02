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
@Table(name = "pharma_product_attribute_cosmetics")
public class ProductAttributeCosmetics {

    @Id
    @Column(name = "product_attribute_id", length = 30)
    private String productAttributeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    @JsonIgnore
    private ProductDetails product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", referencedColumnName = "product_type_id")
    @JsonIgnore
    private ProductType productType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_sub_type_id", referencedColumnName = "product_sub_type_id")
    @JsonIgnore
    private ProductSubType productSubType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_form_id", referencedColumnName = "product_form_id")
    @JsonIgnore
    private ProductForm productForm;

    @Column(name = "variant_name")
    private String VariantName;

    // MANY-TO-MANY with cosmetic_and_intended_use_mapping
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_cosmetic_and_intended_use_mapping",
            joinColumns = @JoinColumn(name = "product_attribute_id"),
            inverseJoinColumns = @JoinColumn(name = "intended_use_area_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "productAttributes"})
    private List<IntendedUseArea> IntendedUseArea = new ArrayList<>();

    //skin type
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_cosmetic_skin_type_mapping",
            joinColumns = @JoinColumn(name = "product_attribute_id"),
            inverseJoinColumns = @JoinColumn(name = "skin_type_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "productAttributes"})
    private List<SkinType> SkinType = new ArrayList<>();

    //Hair type
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_cosmetic_hair_type_mapping",
            joinColumns = @JoinColumn(name = "product_attribute_id"),
            inverseJoinColumns = @JoinColumn(name = "hair_type_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "productAttributes"})
    private List<HairType> hairTypes = new ArrayList<>();

    //join Age master table
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_cosmetic_age_group_mapping",
            joinColumns = @JoinColumn(name = "product_attribute_id"),
            inverseJoinColumns = @JoinColumn(name = "age_group_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<AgeGroup> ageGroups = new ArrayList<>();

    @Column(name = "gender")
    private String Gender;

    @Column(name = "fragrance")
    private String fragrance;

    @Column(name = "net_quantity")
    private Double NetQuantity;

    // FK → Net Quantity Unit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "net_quantity_unit_id", referencedColumnName = "net_quantity_unit_id")
    @JsonIgnore
    private NetQuantityUnit netQuantityUnit;

    @Column(name = "manufacturer_name")
    private String manufacturerName;

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by", length = 30)
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

}
