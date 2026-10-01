package tiameds.pharmabackend.repository.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.pharmabackend.entity.warehouse.WarehouseReturnDetails;

@Repository
public interface WarehouseReturnDetailsRepository extends JpaRepository<WarehouseReturnDetails, Long> {
}
