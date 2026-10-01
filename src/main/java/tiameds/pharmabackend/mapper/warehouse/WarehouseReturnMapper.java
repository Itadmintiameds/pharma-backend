package tiameds.pharmabackend.mapper.warehouse;

import tiameds.pharmabackend.dto.warehouse.WarehouseReturnDetailsDto;
import tiameds.pharmabackend.dto.warehouse.WarehouseReturnDto;
import tiameds.pharmabackend.entity.warehouse.WarehouseReturn;
import tiameds.pharmabackend.entity.warehouse.WarehouseReturnDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WarehouseReturnMapper {

    public static WarehouseReturnDto toDto(WarehouseReturn entity) {

        if (entity == null) {
            return null;
        }

        WarehouseReturnDto dto = new WarehouseReturnDto();

        dto.setWarehouseReturnId(entity.getWarehouseReturnId());
        dto.setFromPharmacyId(entity.getFromPharmacyId());
        dto.setToWarehouseId(entity.getToWarehouseId());
        dto.setStockReturnNo(entity.getStockReturnNo());
        dto.setStockReturnDate(entity.getStockReturnDate());
        dto.setStockReturnType(entity.getStockReturnType());
        dto.setStockReturnStatus(entity.getStockReturnStatus());
        dto.setTotalReturnProducts(entity.getTotalReturnProducts());
        dto.setTotalReturnQuantity(entity.getTotalReturnQuantity());
        dto.setTotalReceivedQuantity(entity.getTotalReceivedQuantity());
        dto.setTotalNotReceivedQuantity(entity.getTotalNotReceivedQuantity());
        dto.setIsDelete(entity.getIsDelete());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        if (entity.getWarehouseReturnDetails() != null) {

            List<WarehouseReturnDetailsDto> details = entity.getWarehouseReturnDetails()
                    .stream()
                    .map(WarehouseReturnDetailsMapper::toDto)
                    .collect(Collectors.toList());

            dto.setWarehouseReturnDetails(details);
        }

        return dto;
    }

    public static WarehouseReturn toEntity(WarehouseReturnDto dto) {

        if (dto == null) {
            return null;
        }

        WarehouseReturn entity = new WarehouseReturn();

        entity.setWarehouseReturnId(dto.getWarehouseReturnId());
        entity.setFromPharmacyId(dto.getFromPharmacyId());
        entity.setToWarehouseId(dto.getToWarehouseId());
        entity.setStockReturnNo(dto.getStockReturnNo());
        entity.setStockReturnDate(dto.getStockReturnDate());
        entity.setStockReturnType(dto.getStockReturnType());
        entity.setStockReturnStatus(dto.getStockReturnStatus());
        entity.setTotalReturnProducts(dto.getTotalReturnProducts());
        entity.setTotalReturnQuantity(dto.getTotalReturnQuantity());
        entity.setTotalReceivedQuantity(dto.getTotalReceivedQuantity());
        entity.setTotalNotReceivedQuantity(dto.getTotalNotReceivedQuantity());
        entity.setIsDelete(dto.getIsDelete() != null ? dto.getIsDelete() : false);
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        if (dto.getWarehouseReturnDetails() != null) {

            List<WarehouseReturnDetails> details = new ArrayList<>();

            for (var detailsDto : dto.getWarehouseReturnDetails()) {
                WarehouseReturnDetails warehouseReturnDetails = WarehouseReturnDetailsMapper.toEntity(detailsDto);
                warehouseReturnDetails.setWarehouseReturn(entity);
                details.add(warehouseReturnDetails);
            }

            entity.setWarehouseReturnDetails(details);
        }

        return entity;
    }
}
