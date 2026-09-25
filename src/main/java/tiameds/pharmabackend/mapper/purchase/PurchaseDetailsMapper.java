package tiameds.pharmabackend.mapper.purchase;

import tiameds.pharmabackend.dto.purchase.PurchaseDetailsDto;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;

public class PurchaseDetailsMapper {

    public static PurchaseDetailsDto toDto(PurchaseDetails entity) {

        if (entity == null) {
            return null;
        }

        PurchaseDetailsDto dto = new PurchaseDetailsDto();

        dto.setPurchaseDetailsId(entity.getPurchaseDetailsId());

        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getProductId());
            dto.setProductName(entity.getProduct().getProductName());
        }

        if (entity.getBatch() != null) {

            dto.setBatchId(entity.getBatch().getBatchId());
            dto.setBatchNumber(entity.getBatch().getBatchNumber());
            dto.setExpiryDate(entity.getBatch().getExpiryDate());

            PackagingDetails packaging = entity.getBatch().getPackagingDetails();

            if (packaging != null) {

                dto.setPurchaseUnit(packaging.getPurchaseUnit());
                dto.setUnitContains(packaging.getPurchaseUnitContains());

                if (packaging.getPurchaseSmallestUnit() != null) {
                    dto.setSmallestUnit(
                            packaging.getPurchaseSmallestUnit().getPurchaseSmallestUnitName());
                }
            }

            // The packaging row is the source of truth, but a batch may carry its
            // own purchase unit when the packaging never defined one.
            if (dto.getPurchaseUnit() == null) {
                dto.setPurchaseUnit(entity.getBatch().getPurchaseUnit());
            }
        }

        dto.setPurchaseQuantity(entity.getPurchaseQuantity());
        dto.setFreeUnit(entity.getFreeUnit());
        dto.setFreeQuantity(entity.getFreeQuantity());
        dto.setGrossAmount(entity.getGrossAmount());
        dto.setGst(entity.getGst());
        dto.setGstPercentage(entity.getGstPercentage());
        dto.setNetAmount(entity.getNetAmount());
        dto.setReturnDetailsStatus(entity.getReturnDetailsStatus());
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
        entity.setGstPercentage(dto.getGstPercentage());
        entity.setNetAmount(dto.getNetAmount());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }
}