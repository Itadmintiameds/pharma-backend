package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
