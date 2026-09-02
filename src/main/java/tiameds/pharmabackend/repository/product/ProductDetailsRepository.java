package tiameds.pharmabackend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.product.ProductDetails;

import java.util.List;

@Repository
public interface ProductDetailsRepository extends JpaRepository<ProductDetails, String> {

    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(product_id, LENGTH(product_id) - 4, 5) AS INTEGER))
        FROM pharma_product_details
    """, nativeQuery = true)
    Integer findMaxProductNumber();

    // all products belonging to a single pharmacy
    // OLD (single-pharmacy ManyToOne, property no longer exists):
    // List<ProductDetails> findByPharmacy_PharmacyId(String pharmacyId);

    // Product now maps to many pharmacies via ManyToMany -> traverse the collection.
    List<ProductDetails> findByPharmacies_PharmacyId(String pharmacyId);

    // all products mapped to a single warehouse (Product <-> Warehouse ManyToMany)
    List<ProductDetails> findByWarehouses_WarehouseId(String warehouseId);

    // Shared org catalog: all products owned by one organization.
    List<ProductDetails> findByOrganization_OrganizationId(Long organizationId);

    // Per-org catalog de-dup guard. Mirrors the DB unique constraint
    // (organization_id, product_name, brand_name, hsn_no). Note: like the DB
    // constraint, null brand/hsn are treated as distinct and won't collide here.
    boolean existsByOrganization_OrganizationIdAndProductNameAndBrandNameAndHsnNo(
            Long organizationId, String productName, String brandName, String hsnNo);
}
