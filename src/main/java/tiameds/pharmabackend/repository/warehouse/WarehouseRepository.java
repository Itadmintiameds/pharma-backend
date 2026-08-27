package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.Warehouse;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {

    List<Warehouse> findAllByOrganization_OrganizationIdOrderByWarehouseName(Long organizationId);

    @Query("""
       SELECT w.warehouseId
       FROM Warehouse w
       ORDER BY w.createdAt DESC
       LIMIT 1
       """)
    String findLatestWarehouseId();

    // True when the warehouse belongs to the same organization as the user.
    // Used to authorize a SUPER ADMIN operating on a warehouse (X-Warehouse-Id):
    // the superadmin need not be mapped to the warehouse via pharma_user_warehouse,
    // only be in the warehouse's organization.
    @Query("""
       SELECT COUNT(w) > 0
       FROM Warehouse w, UserDetails u
       WHERE w.warehouseId = :warehouseId
         AND u.userId = :userId
         AND w.organization.organizationId = u.organization.organizationId
       """)
    boolean existsWarehouseInUserOrganization(
            @Param("warehouseId") String warehouseId,
            @Param("userId") String userId);
}
