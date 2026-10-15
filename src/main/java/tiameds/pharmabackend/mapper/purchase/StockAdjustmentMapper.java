package tiameds.pharmabackend.mapper.purchase;

import tiameds.pharmabackend.dto.purchase.StockAdjustmentDto;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.StockAdjustment;

public class StockAdjustmentMapper {

    public static StockAdjustmentDto toDto(StockAdjustment entity) {

        if (entity == null) {
            return null;
        }

        StockAdjustmentDto dto = new StockAdjustmentDto();

        dto.setStockAdjustmentId(entity.getStockAdjustmentId());
        dto.setPharmacyId(entity.getPharmacyId());
        dto.setWarehouseId(entity.getWarehouseId());

        ProductDetails product = entity.getProduct();

        if (product != null) {
            dto.setProductId(product.getProductId());
            dto.setProductName(product.getProductName());
        }

        BatchDetails batch = entity.getBatch();

        if (batch != null) {
            dto.setBatchId(batch.getBatchId());
            dto.setBatchNumber(batch.getBatchNumber());
        }

        dto.setStockAdjustmentNo(entity.getStockAdjustmentNo());
        dto.setStockAdjustmentDate(entity.getStockAdjustmentDate());
        dto.setActualStockQty(entity.getActualStockQty());
        dto.setAdjustmentQty(entity.getAdjustmentQty());
        dto.setAdjustmentType(entity.getAdjustmentType());
        dto.setAdjustmentValue(entity.getAdjustmentValue());
        dto.setStockAdjustmentReason(entity.getStockAdjustmentReason());
        dto.setStockAdjustmentRemarks(entity.getStockAdjustmentRemarks());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    public static StockAdjustment toEntity(StockAdjustmentDto dto) {

        if (dto == null) {
            return null;
        }

        StockAdjustment entity = new StockAdjustment();

        entity.setStockAdjustmentId(dto.getStockAdjustmentId());
        entity.setPharmacyId(dto.getPharmacyId());
        entity.setWarehouseId(dto.getWarehouseId());

        if (dto.getProductId() != null) {
            ProductDetails product = new ProductDetails();
            product.setProductId(dto.getProductId());
            entity.setProduct(product);
        }

        if (dto.getBatchId() != null) {
            BatchDetails batch = new BatchDetails();
            batch.setBatchId(dto.getBatchId());
            entity.setBatch(batch);
        }

        entity.setStockAdjustmentNo(dto.getStockAdjustmentNo());
        entity.setStockAdjustmentDate(dto.getStockAdjustmentDate());
        entity.setActualStockQty(dto.getActualStockQty());
        entity.setAdjustmentQty(dto.getAdjustmentQty());
        entity.setAdjustmentType(dto.getAdjustmentType());
        entity.setAdjustmentValue(dto.getAdjustmentValue());
        entity.setStockAdjustmentReason(dto.getStockAdjustmentReason());
        entity.setStockAdjustmentRemarks(dto.getStockAdjustmentRemarks());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }
}
