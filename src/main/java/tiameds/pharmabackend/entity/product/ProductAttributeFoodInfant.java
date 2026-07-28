package tiameds.pharmabackend.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.master.AgeGroup;
import tiameds.pharmabackend.entity.master.ProductForm;
import tiameds.pharmabackend.entity.master.ProductSubType;
import tiameds.pharmabackend.entity.master.ProductType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_product_attribute_food_infant")
public class ProductAttributeFoodInfant {

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

    //join Age master table
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_cosmetic_age_group_mapping",
            joinColumns = @JoinColumn(name = "product_attribute_id"),
            inverseJoinColumns = @JoinColumn(name = "age_group_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<AgeGroup> ageGroups = new ArrayList<>();

    @Column(name = "net_quantity")
    private Double NetQuantity;

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
