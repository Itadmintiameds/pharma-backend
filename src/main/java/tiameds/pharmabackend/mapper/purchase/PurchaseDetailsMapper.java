package tiameds.pharmabackend.mapper.purchase;

import tiameds.pharmabackend.dto.purchase.PurchaseDetailsDto;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;

public class PurchaseDetailsMapper {

    public static PurchaseDetailsDto toDto(PurchaseDetails entity) {

        if (entity == null) {
            return null;
        }

        PurchaseDetailsDto dto = new PurchaseDetailsDto();

        dto.setPurchaseDetailsId(entity.getPurchaseDetailsId());
        dto.setProductId(entity.getProduct().getProductId());
        dto.setBatchId(entity.getBatch().getBatchId());
        dto.setPurchaseQuantity(entity.getPurchaseQuantity());
        dto.setFreeUnit(entity.getFreeUnit());
        dto.setFreeQuantity(entity.getFreeQuantity());
        dto.setGrossAmount(entity.getGrossAmount());
        dto.setGst(entity.getGst());
        dto.setNetAmount(entity.getNetAmount());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    public static PurchaseDetails toEntity(PurchaseDetailsDto dto) {

        if (dto == null) {
            return null;
        }

        PurchaseDetails entity = new PurchaseDetails();

        entity.setPurchaseDetailsId(dto.getPurchaseDetailsId());

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

        entity.setPurchaseQuantity(dto.getPurchaseQuantity());
        entity.setFreeUnit(dto.getFreeUnit());
        entity.setFreeQuantity(dto.getFreeQuantity());
        entity.setGrossAmount(dto.getGrossAmount());
        entity.setGst(dto.getGst());
        entity.setNetAmount(dto.getNetAmount());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }
}