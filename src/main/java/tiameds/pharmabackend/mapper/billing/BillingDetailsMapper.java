package tiameds.pharmabackend.mapper.billing;

import tiameds.pharmabackend.dto.billing.BillingDetailsDto;
import tiameds.pharmabackend.entity.billing.BillingDetails;

public class BillingDetailsMapper {

    public static BillingDetailsDto toDto(BillingDetails entity) {

        if (entity == null) {
            return null;
        }

        BillingDetailsDto dto = new BillingDetailsDto();

        dto.setBillingDetailsId(entity.getBillingDetailsId());
        dto.setBillingId(
                entity.getBilling() != null
                        ? entity.getBilling().getBillingId()
                        : null
        );

        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getProductId());
            dto.setProductName(entity.getProduct().getProductName());
            dto.setHsnNo(entity.getProduct().getHsnNo());
            dto.setGstPercentage(entity.getProduct().getGstPercentage());
        }

        if (entity.getBatch() != null) {
            dto.setBatchId(entity.getBatch().getBatchId());
            dto.setBatchNumber(entity.getBatch().getBatchNumber());
            dto.setExpiryDate(entity.getBatch().getExpiryDate());
            dto.setMrpPerUnit(entity.getBatch().getMrpPerUnit());
        }

        dto.setUnit(entity.getUnit());
        dto.setBillQuantity(entity.getBillQuantity());
        dto.setGrossAmount(entity.getGrossAmount());
        dto.setTotalMrpAmountPerUnit(entity.getTotalMrpAmountPerUnit());
        dto.setDiscountPercentage(entity.getDiscountPercentage());
        dto.setDiscountAmount(entity.getDiscountAmount());
        dto.setGstAmount(entity.getGstAmount());
        dto.setNetAmount(entity.getNetAmount());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    public static BillingDetails toEntity(BillingDetailsDto dto) {

        if (dto == null) {
            return null;
        }

        BillingDetails entity = new BillingDetails();

        entity.setBillingDetailsId(dto.getBillingDetailsId());

        // product and batch are resolved and attached by the service

        entity.setUnit(dto.getUnit());
        entity.setBillQuantity(dto.getBillQuantity());
        entity.setGrossAmount(dto.getGrossAmount());
        entity.setTotalMrpAmountPerUnit(dto.getTotalMrpAmountPerUnit());
        entity.setDiscountPercentage(dto.getDiscountPercentage());
        entity.setDiscountAmount(dto.getDiscountAmount());
        entity.setGstAmount(dto.getGstAmount());
        entity.setNetAmount(dto.getNetAmount());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }
}