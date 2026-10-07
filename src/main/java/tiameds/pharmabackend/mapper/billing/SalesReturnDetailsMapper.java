package tiameds.pharmabackend.mapper.billing;

import tiameds.pharmabackend.dto.billing.SalesReturnDetailsDto;
import tiameds.pharmabackend.entity.billing.SalesReturnDetails;

public class SalesReturnDetailsMapper {

    public static SalesReturnDetailsDto toDto(SalesReturnDetails entity) {

        if (entity == null) {
            return null;
        }

        SalesReturnDetailsDto dto = new SalesReturnDetailsDto();

        dto.setSalesReturnDetailId(entity.getSalesReturnDetailId());

        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getProductId());
            dto.setProductName(entity.getProduct().getProductName());
        }

        if (entity.getBatch() != null) {
            dto.setBatchId(entity.getBatch().getBatchId());
            dto.setBatchNumber(entity.getBatch().getBatchNumber());
        }

        dto.setSalesReturnQuantity(entity.getSalesReturnQuantity());
        dto.setSalesReturnReason(entity.getSalesReturnReason());
        dto.setGrossAmount(entity.getGrossAmount());
        dto.setGstAmount(entity.getGstAmount());
        dto.setNetAmount(entity.getNetAmount());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }
}
