package tiameds.pharmabackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.PharmacyOrganization;

@Repository
public interface PharmacyOrganizationRepository extends JpaRepository <PharmacyOrganization, Long> {

    // Organizations whose id/name, or any of whose pharmacies' or warehouses' id/name,
    // match the lower-cased LIKE pattern. Pass "%" to match everything.
    @Query(value = """
       SELECT o
       FROM PharmacyOrganization o
       WHERE LOWER(COALESCE(o.organizationName, '')) LIKE :pattern
          OR CAST(o.organizationId AS String) LIKE :pattern
          OR EXISTS (
               SELECT 1 FROM PharmacyDetails p
               WHERE p.organization = o
                 AND (LOWER(p.pharmacyId) LIKE :pattern
                      OR LOWER(COALESCE(p.pharmacyName, '')) LIKE :pattern))
          OR EXISTS (
               SELECT 1 FROM Warehouse w
               WHERE w.organization = o
                 AND (LOWER(w.warehouseId) LIKE :pattern
                      OR LOWER(COALESCE(w.warehouseName, '')) LIKE :pattern))
       """,
            countQuery = """
       SELECT COUNT(o)
       FROM PharmacyOrganization o
       WHERE LOWER(COALESCE(o.organizationName, '')) LIKE :pattern
          OR CAST(o.organizationId AS String) LIKE :pattern
          OR EXISTS (
               SELECT 1 FROM PharmacyDetails p
               WHERE p.organization = o
                 AND (LOWER(p.pharmacyId) LIKE :pattern
                      OR LOWER(COALESCE(p.pharmacyName, '')) LIKE :pattern))
          OR EXISTS (
               SELECT 1 FROM Warehouse w
               WHERE w.organization = o
                 AND (LOWER(w.warehouseId) LIKE :pattern
                      OR LOWER(COALESCE(w.warehouseName, '')) LIKE :pattern))
       """)
    Page<PharmacyOrganization> searchDirectory(@Param("pattern") String pattern, Pageable pageable);
}
