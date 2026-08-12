package tiameds.pharmabackend.mapper.warehouse;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.warehouse.WarehouseDto;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.entity.product.ProductDetails;

import java.util.stream.Collectors;

@Component
public class WarehouseMapper {

    public Warehouse toEntity(WarehouseDto dto) {

        if (dto == null) {
            return null;
        }

        Warehouse warehouse = new Warehouse();

        warehouse.setWarehouseId(dto.getWarehouseId());
        warehouse.setWarehouseName(dto.getWarehouseName());
        warehouse.setWarehouseCode(dto.getWarehouseCode());
        warehouse.setWarehouseAddress(dto.getWarehouseAddress());
        warehouse.setContactPersonName(dto.getContactPersonName());
        warehouse.setMobileNumber(dto.getMobileNumber());
        warehouse.setIsActive(dto.getIsActive());
        warehouse.setCreatedBy(dto.getCreatedBy());
        warehouse.setCreatedAt(dto.getCreatedAt());
        warehouse.setModifiedBy(dto.getModifiedBy());
        warehouse.setModifiedAt(dto.getModifiedAt());

        return warehouse;
    }

    public WarehouseDto toDto(Warehouse entity) {

        if (entity == null) {
            return null;
        }

        WarehouseDto dto = new WarehouseDto();

        dto.setWarehouseId(entity.getWarehouseId());
        dto.setWarehouseName(entity.getWarehouseName());
        dto.setWarehouseCode(entity.getWarehouseCode());
        dto.setWarehouseAddress(entity.getWarehouseAddress());
        dto.setContactPersonName(entity.getContactPersonName());
        dto.setMobileNumber(entity.getMobileNumber());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        if (entity.getOrganization() != null) {
            dto.setOrganizationId(entity.getOrganization().getOrganizationId());
        }

        if (entity.getProducts() != null) {
            dto.setProductIds(
                    entity.getProducts()
                            .stream()
                            .map(ProductDetails::getProductId)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }
}
