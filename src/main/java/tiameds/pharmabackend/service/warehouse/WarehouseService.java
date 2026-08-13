package tiameds.pharmabackend.service.warehouse;

import tiameds.pharmabackend.dto.warehouse.WarehouseDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface WarehouseService {

    WarehouseDto createWarehouse(WarehouseDto warehouseDto, UserDetails user);

    WarehouseDto updateWarehouse(String warehouseId, WarehouseDto warehouseDto, UserDetails user);

    WarehouseDto getWarehouseById(String warehouseId);

    List<WarehouseDto> getWarehousesForUser(UserDetails user);

    List<WarehouseDto> getWarehousesByOrganizationId(Long organizationId, UserDetails user);

    void deleteWarehouse(String warehouseId);
}
