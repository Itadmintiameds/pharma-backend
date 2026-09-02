package tiameds.pharmabackend.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.repository.PharmacyOrganizationRepository;
import tiameds.pharmabackend.service.OrganizationDeletionService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hard-deletes an entire organization and every row that transitively belongs to it,
 * using native bulk DELETEs run in a single transaction, in FK-safe order.
 *
 * Native SQL is used deliberately (instead of JPA cascade) so the delete order is
 * explicit and deterministic, covers join tables that have no JPA entity, and does not
 * depend on Hibernate cascade / persistence-context flush timing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationDeletionServiceImpl implements OrganizationDeletionService {

    @PersistenceContext
    private EntityManager em;

    private final PharmacyOrganizationRepository organizationRepository;

    // Reusable id-scope subqueries (bound via :orgId).
    private static final String ORG_PHARMACIES =
            "SELECT pharmacy_id FROM pharma_pharmacy_details WHERE organization_id = :orgId";
    private static final String ORG_PURCHASES =
            "SELECT purchase_id FROM pharma_purchase WHERE pharmacy_id IN (" + ORG_PHARMACIES + ")";
    private static final String ORG_USERS =
            "SELECT user_id FROM pharma_user_details WHERE organization_id = :orgId";

    @Override
    public Map<String, Integer> deleteOrganizationData(Long organizationId) {

        if (!organizationRepository.existsById(organizationId)) {
            throw new RuntimeException("Organization not found with id: " + organizationId);
        }

        // Capture the product ids owned by this org BEFORE the product<->location join
        // tables (which we use to identify them) get deleted below.
        @SuppressWarnings("unchecked")
        List<String> productIds = em.createNativeQuery(
                        "SELECT product_id FROM pharma_pharmacy_product WHERE pharmacy_id IN (" + ORG_PHARMACIES + ") " +
                        "UNION " +
                        "SELECT product_id FROM pharma_warehouse_product WHERE warehouse_id IN " +
                        "  (SELECT warehouse_id FROM pharma_warehouse WHERE organization_id = :orgId)")
                .setParameter("orgId", organizationId)
                .getResultList();

        Map<String, Integer> deleted = new LinkedHashMap<>();

        // ---- 1. inventory & audit (reference pharmacy/product/batch/packaging/purchase_details) ----
        deleted.put("pharma_inventory_audit", execOrg(
                "DELETE FROM pharma_inventory_audit WHERE pharmacy_id IN (" + ORG_PHARMACIES + ")", organizationId));
        deleted.put("pharma_inventory", execOrg(
                "DELETE FROM pharma_inventory WHERE pharmacy_id IN (" + ORG_PHARMACIES + ")", organizationId));

        // ---- 2. purchases & suppliers ----
        deleted.put("pharma_supplier_payment", execOrg(
                "DELETE FROM pharma_supplier_payment WHERE purchase_id IN (" + ORG_PURCHASES + ")", organizationId));
        deleted.put("pharma_purchase_details(by purchase)", execOrg(
                "DELETE FROM pharma_purchase_details WHERE purchase_id IN (" + ORG_PURCHASES + ")", organizationId));
        deleted.put("pharma_purchase", execOrg(
                "DELETE FROM pharma_purchase WHERE pharmacy_id IN (" + ORG_PHARMACIES + ")", organizationId));
        deleted.put("pharma_supplier_master", execOrg(
                "DELETE FROM pharma_supplier_master WHERE pharmacy_id IN (" + ORG_PHARMACIES + ")", organizationId));

        // ---- 3. product subtree (only when the org actually owns products) ----
        if (!productIds.isEmpty()) {

            // 3a. product-attribute mapping join tables (owning side = attribute) -> clear first
            deleted.put("pharma_cosmetic_and_intended_use_mapping", execPids(
                    "DELETE FROM pharma_cosmetic_and_intended_use_mapping WHERE product_attribute_id IN " +
                    "(SELECT product_attribute_id FROM pharma_product_attribute_cosmetics WHERE product_id IN (:pids))", productIds));
            deleted.put("pharma_cosmetic_skin_type_mapping", execPids(
                    "DELETE FROM pharma_cosmetic_skin_type_mapping WHERE product_attribute_id IN " +
                    "(SELECT product_attribute_id FROM pharma_product_attribute_cosmetics WHERE product_id IN (:pids))", productIds));
            deleted.put("pharma_cosmetic_hair_type_mapping", execPids(
                    "DELETE FROM pharma_cosmetic_hair_type_mapping WHERE product_attribute_id IN " +
                    "(SELECT product_attribute_id FROM pharma_product_attribute_cosmetics WHERE product_id IN (:pids))", productIds));
            deleted.put("pharma_cosmetic_age_group_mapping", execPids(
                    "DELETE FROM pharma_cosmetic_age_group_mapping WHERE product_attribute_id IN " +
                    "(SELECT product_attribute_id FROM pharma_product_attribute_cosmetics WHERE product_id IN (:pids))", productIds));
            deleted.put("pharma_food_infant_age_group_mapping", execPids(
                    "DELETE FROM pharma_food_infant_age_group_mapping WHERE product_attribute_id IN " +
                    "(SELECT product_attribute_id FROM pharma_product_attribute_food_infant WHERE product_id IN (:pids))", productIds));
            deleted.put("pharma_consumable_material_mapping", execPids(
                    "DELETE FROM pharma_consumable_material_mapping WHERE product_attribute_id IN " +
                    "(SELECT product_attribute_id FROM pharma_product_attribute_consumable_medical WHERE product_id IN (:pids))", productIds));
            deleted.put("pharma_non_consumable_material_mapping", execPids(
                    "DELETE FROM pharma_non_consumable_material_mapping WHERE product_attribute_id IN " +
                    "(SELECT product_attribute_id FROM pharma_product_attribute_non_consumable_medical WHERE product_id IN (:pids))", productIds));

            // 3b. product_molecule (child of the drug attribute) -> before the drug attribute
            deleted.put("pharma_product_molecule", execPids(
                    "DELETE FROM pharma_product_molecule WHERE product_attribute_id IN " +
                    "(SELECT product_attribute_id FROM pharma_product_attribute_drug WHERE product_id IN (:pids))", productIds));

            // 3c. attribute tables
            deleted.put("pharma_product_attribute_cosmetics", execPids(
                    "DELETE FROM pharma_product_attribute_cosmetics WHERE product_id IN (:pids)", productIds));
            deleted.put("pharma_product_attribute_food_infant", execPids(
                    "DELETE FROM pharma_product_attribute_food_infant WHERE product_id IN (:pids)", productIds));
            deleted.put("pharma_product_attribute_consumable_medical", execPids(
                    "DELETE FROM pharma_product_attribute_consumable_medical WHERE product_id IN (:pids)", productIds));
            deleted.put("pharma_product_attribute_non_consumable_medical", execPids(
                    "DELETE FROM pharma_product_attribute_non_consumable_medical WHERE product_id IN (:pids)", productIds));
            deleted.put("pharma_product_attribute_supplements", execPids(
                    "DELETE FROM pharma_product_attribute_supplements WHERE product_id IN (:pids)", productIds));
            deleted.put("pharma_product_attribute_drug", execPids(
                    "DELETE FROM pharma_product_attribute_drug WHERE product_id IN (:pids)", productIds));

            // 3d. any purchase_details still referencing these products (safety net) -> before batch/packaging/product
            deleted.put("pharma_purchase_details(by product)", execPids(
                    "DELETE FROM pharma_purchase_details WHERE product_id IN (:pids)", productIds));

            // 3e. batch (references packaging) then packaging
            deleted.put("pharma_batch_details", execPids(
                    "DELETE FROM pharma_batch_details WHERE product_id IN (:pids)", productIds));
            deleted.put("pharma_packaging_details", execPids(
                    "DELETE FROM pharma_packaging_details WHERE product_id IN (:pids)", productIds));

            // 3f. product<->location join tables (before products & before pharmacies/warehouses)
            deleted.put("pharma_pharmacy_product", execPids(
                    "DELETE FROM pharma_pharmacy_product WHERE product_id IN (:pids)", productIds));
            deleted.put("pharma_warehouse_product", execPids(
                    "DELETE FROM pharma_warehouse_product WHERE product_id IN (:pids)", productIds));

            // 3g. finally the products themselves
            deleted.put("pharma_product_details", execPids(
                    "DELETE FROM pharma_product_details WHERE product_id IN (:pids)", productIds));
        }

        // ---- 4. user subtree ----
        deleted.put("pharma_user_feature_permission", execOrg(
                "DELETE FROM pharma_user_feature_permission WHERE user_id IN (" + ORG_USERS + ")", organizationId));
        deleted.put("pharma_refresh_token", execOrg(
                "DELETE FROM pharma_refresh_token WHERE user_id IN (" + ORG_USERS + ")", organizationId));
        deleted.put("pharma_otp", execOrg(
                "DELETE FROM pharma_otp WHERE user_id IN (" + ORG_USERS + ")", organizationId));
        // consent ledger: FK -> pharma_user_details, so it must go before the users
        deleted.put("pharma_user_policy_acceptance", execOrg(
                "DELETE FROM pharma_user_policy_acceptance WHERE user_id IN (" + ORG_USERS + ")", organizationId));
        // user<->pharmacy join: clear rows for this org's users OR this org's pharmacies
        deleted.put("pharma_user_pharmacy", execOrg(
                "DELETE FROM pharma_user_pharmacy WHERE user_id IN (" + ORG_USERS + ") " +
                "OR pharmacy_id IN (" + ORG_PHARMACIES + ")", organizationId));

        // ---- 5. documents ----
        deleted.put("pharma_documents", execOrg(
                "DELETE FROM pharma_documents WHERE pharmacy_id IN (" + ORG_PHARMACIES + ")", organizationId));

        // ---- 6. org-owned roots (each has an organization_id FK -> delete before the org) ----
        deleted.put("pharma_user_details", execOrg(
                "DELETE FROM pharma_user_details WHERE organization_id = :orgId", organizationId));
        deleted.put("pharma_warehouse", execOrg(
                "DELETE FROM pharma_warehouse WHERE organization_id = :orgId", organizationId));
        deleted.put("pharma_pharmacy_details", execOrg(
                "DELETE FROM pharma_pharmacy_details WHERE organization_id = :orgId", organizationId));

        // ---- 7. the organization itself ----
        deleted.put("pharma_organization_details", execOrg(
                "DELETE FROM pharma_organization_details WHERE organization_id = :orgId", organizationId));

        log.warn("Hard-deleted all data for organization {}. Rows deleted per table: {}", organizationId, deleted);
        return deleted;
    }

    private int execOrg(String sql, Long orgId) {
        return em.createNativeQuery(sql).setParameter("orgId", orgId).executeUpdate();
    }

    private int execPids(String sql, List<String> productIds) {
        return em.createNativeQuery(sql).setParameter("pids", productIds).executeUpdate();
    }
}
