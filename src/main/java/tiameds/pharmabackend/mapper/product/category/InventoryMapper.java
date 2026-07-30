package tiameds.pharmabackend.mapper.product.category;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import tiameds.pharmabackend.dto.product.BatchDetailsDto;
import tiameds.pharmabackend.dto.product.PackagingDetailsDto;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;

@Component
public class InventoryMapper {

    public BatchDetails toEntity(BatchDetailsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        BatchDetails entity = new BatchDetails();
        if (dto.getPackagingId() != null) {
            PackagingDetails pd = new PackagingDetails();
            pd.setPackagingId(dto.getPackagingId());
            entity.setPackagingDetails(pd);
        }
        entity.setBatchNumber(dto.getBatchNumber());
        entity.setManufacturingDate(dto.getManufacturingDate());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setPurchaseUnit(dto.getPurchaseUnit());
        entity.setStockQuantity(dto.getStockQuantity());
        entity.setFreeUnit(dto.getFreeUnit());
        entity.setFreeQuantity(dto.getFreeQuantity());
        entity.setPurchasePrice(dto.getPurchasePrice());
        entity.setMrp(dto.getMrp());
        entity.setSellingPrice(dto.getSellingPrice());
        entity.setPurchasePricePerUnit(dto.getPurchasePricePerUnit());
        entity.setMrpPerUnit(dto.getMrpPerUnit());
        entity.setSellingPricePerUnit(dto.getSellingPricePerUnit());
        entity.setRackLocation(dto.getRackLocation());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    public PackagingDetails toEntity(PackagingDetailsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        PackagingDetails entity = new PackagingDetails();
        entity.setPurchaseUnit(dto.getPurchaseUnit());
        entity.setPurchaseUnitContains(dto.getPurchaseUnitContains());
        entity.setSmallestUnit(dto.getSmallestUnit());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    public BatchDetailsDto toDto(BatchDetails entity) {
        if (entity == null) return null;
        BatchDetailsDto dto = new BatchDetailsDto();
        dto.setBatchId(entity.getBatchId());
        if (entity.getPackagingDetails() != null) {
            dto.setPackagingId(entity.getPackagingDetails().getPackagingId());
        }
        dto.setBatchNumber(entity.getBatchNumber());
        dto.setManufacturingDate(entity.getManufacturingDate());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setPurchaseUnit(entity.getPurchaseUnit());
        dto.setStockQuantity(entity.getStockQuantity());
        dto.setFreeUnit(entity.getFreeUnit());
        dto.setFreeQuantity(entity.getFreeQuantity());
        dto.setPurchasePrice(entity.getPurchasePrice());
        dto.setMrp(entity.getMrp());
        dto.setSellingPrice(entity.getSellingPrice());
        dto.setPurchasePricePerUnit(entity.getPurchasePricePerUnit());
        dto.setMrpPerUnit(entity.getMrpPerUnit());
        dto.setSellingPricePerUnit(entity.getSellingPricePerUnit());
        dto.setRackLocation(entity.getRackLocation());
        return dto;
    }

    public PackagingDetailsDto toDto(PackagingDetails entity) {
        if (entity == null) return null;
        PackagingDetailsDto dto = new PackagingDetailsDto();
        dto.setPackagingId(entity.getPackagingId());
        dto.setPurchaseUnit(entity.getPurchaseUnit());
        dto.setPurchaseUnitContains(entity.getPurchaseUnitContains());
        dto.setSmallestUnit(entity.getSmallestUnit());
        return dto;
    }

}
