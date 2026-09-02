package tiameds.pharmabackend.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.entity.master.ProductCategory;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pharma_product_details",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_product_org_name_brand_hsn",
                columnNames = {"organization_id", "product_name", "brand_name", "hsn_no"}))
public class ProductDetails {

    @Id
    @Column(name = "product_id", length = 30)
    private String productId;

    // Catalog owner: a product is defined once per organization and shared across
    // all of that organization's locations. Stock stays per-location (Inventory /
    // WarehouseInventory); the pharmacy/warehouse ManyToMany below is now an
    // assortment ("which locations carry this product"), not ownership.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "organization_id")
    @JsonIgnore
    private PharmacyOrganization organization;

    // OLD: single-pharmacy mapping (ManyToOne). Replaced by ManyToMany below so a
    // product can be shared across multiple pharmacies and warehouses.
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "pharmacy_id", referencedColumnName = "pharmacy_id")
    // @JsonIgnore
    // private PharmacyDetails pharmacy;

    // Product <-> Pharmacy ManyToMany (this side owns the join table)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_pharmacy_product",
            joinColumns = @JoinColumn(name = "product_id", referencedColumnName = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "pharmacy_id", referencedColumnName = "pharmacy_id")
    )
    @JsonIgnore
    private List<PharmacyDetails> pharmacies = new ArrayList<>();

    // Product <-> Warehouse ManyToMany (this side owns the join table)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pharma_warehouse_product",
            joinColumns = @JoinColumn(name = "product_id", referencedColumnName = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "warehouse_id", referencedColumnName = "warehouse_id")
    )
    @JsonIgnore
    private List<Warehouse> warehouses = new ArrayList<>();

    //join product category master table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id", referencedColumnName = "product_category_id")
    @JsonIgnore
    private ProductCategory productCategory;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage;

    @Column(name = "hsn_no", length = 20)
    private String hsnNo;

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_by", length = 30)
    private String modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PackagingDetails> packagingDetails;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<BatchDetails> batchDetails;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ProductAttributeSupplements> productAttributeSupplements;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ProductAttributeDrug> productAttributeDrugs;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttributeCosmetics> productAttributeCosmetics;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttributeFoodInfant> productAttributeFoodInfants;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttributeConsumableMedical> productAttributeConsumableMedicals;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttributeNonConsumableMedical> productAttributeNonConsumableMedicals;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<PurchaseDetails> purchaseDetails = new ArrayList<>();
}
