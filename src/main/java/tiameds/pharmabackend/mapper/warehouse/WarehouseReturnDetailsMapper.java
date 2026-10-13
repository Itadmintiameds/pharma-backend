package tiameds.pharmabackend.mapper.warehouse;

import tiameds.pharmabackend.dto.warehouse.WarehouseReturnDetailsDto;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.warehouse.WarehouseReturnDetails;

public class WarehouseReturnDetailsMapper {

    public static WarehouseReturnDetailsDto toDto(WarehouseReturnDetails entity) {

        if (entity == null) {
            return null;
        }

        WarehouseReturnDetailsDto dto = new WarehouseReturnDetailsDto();

        dto.setWarehouseReturnDetailId(entity.getWarehouseReturnDetailId());

        if (entity.getDistributionLine() != null) {
            dto.setWarehouseDistributionDetailsId(
                    entity.getDistributionLine().getWarehouseDistributionDetailsId());
        }

        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getProductId());
            dto.setProductName(entity.getProduct().getProductName());
        }

        if (entity.getBatch() != null) {
            dto.setBatchId(entity.getBatch().getBatchId());
            dto.setBatchNumber(entity.getBatch().getBatchNumber());

            PackagingDetails packaging = entity.getBatch().getPackagingDetails();

            if (packaging != null) {
                dto.setPurchaseUnit(packaging.getPurchaseUnit());
                dto.setPurchaseUnitContains(packaging.getPurchaseUnitContains());

                if (packaging.getPurchaseSmallestUnit() != null) {
                    dto.setSmallestUnit(
                            packaging.getPurchaseSmallestUnit().getPurchaseSmallestUnitName());
                }
            }

            // Fall back to the batch's own purchase unit when packaging has none.
            if (dto.getPurchaseUnit() == null) {
                dto.setPurchaseUnit(entity.getBatch().getPurchaseUnit());
            }
        }

        dto.setReturnQuantity(entity.getReturnQuantity());
        dto.setDispatchQuantity(entity.getDispatchQuantity());
        dto.setReceivedQuantity(entity.getReceivedQuantity());
        dto.setNotReceivedQuantity(entity.getNotReceivedQuantity());
        dto.setReturnReason(entity.getReturnReason());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    public static WarehouseReturnDetails toEntity(WarehouseReturnDetailsDto dto) {

        if (dto == null) {
            return null;
        }

        WarehouseReturnDetails entity = new WarehouseReturnDetails();

        entity.setWarehouseReturnDetailId(dto.getWarehouseReturnDetailId());

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

        entity.setReturnQuantity(dto.getReturnQuantity());
        entity.setDispatchQuantity(dto.getDispatchQuantity());
        entity.setReceivedQuantity(dto.getReceivedQuantity());
        entity.setNotReceivedQuantity(dto.getNotReceivedQuantity());
        entity.setReturnReason(dto.getReturnReason());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }
}
