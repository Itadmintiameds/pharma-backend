package tiameds.pharmabackend.mapper.purchase;

import tiameds.pharmabackend.dto.purchase.PurchaseReturnDetailsDto;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.PurchaseReturnDetails;

public class PurchaseReturnDetailsMapper {

    public static PurchaseReturnDetailsDto toDto(PurchaseReturnDetails entity) {

        if (entity == null) {
            return null;
        }

        PurchaseReturnDetailsDto dto = new PurchaseReturnDetailsDto();

        dto.setPurchaseReturnDetailId(entity.getPurchaseReturnDetailId());

        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getProductId());
            dto.setProductName(entity.getProduct().getProductName());
        }

        if (entity.getBatch() != null) {
            dto.setBatchId(entity.getBatch().getBatchId());
            dto.setBatchNumber(entity.getBatch().getBatchNumber());
        }

        dto.setPurchaseReturnQuantity(entity.getPurchaseReturnQuantity());
        dto.setFreeReturnQuantity(entity.getFreeReturnQuantity());
        dto.setReturnReason(entity.getReturnReason());
        dto.setGrossAmount(entity.getGrossAmount());
        dto.setGstAmount(entity.getGstAmount());
        dto.setNetAmount(entity.getNetAmount());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    public static PurchaseReturnDetails toEntity(PurchaseReturnDetailsDto dto) {

        if (dto == null) {
            return null;
        }

        PurchaseReturnDetails entity = new PurchaseReturnDetails();

        entity.setPurchaseReturnDetailId(dto.getPurchaseReturnDetailId());

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

        entity.setPurchaseReturnQuantity(dto.getPurchaseReturnQuantity());
        entity.setFreeReturnQuantity(dto.getFreeReturnQuantity());
        entity.setReturnReason(dto.getReturnReason());
        entity.setGrossAmount(dto.getGrossAmount());
        entity.setGstAmount(dto.getGstAmount());
        entity.setNetAmount(dto.getNetAmount());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }
}
