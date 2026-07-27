package tiameds.pharmabackend.mapper.supplier;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.supplier.SupplierPaymentDto;
import tiameds.pharmabackend.entity.supplier.SupplierPayment;

@Component
public class SupplierPaymentMapper {

    // Entity -> DTO
    public SupplierPaymentDto toDto(SupplierPayment entity) {
        if (entity == null) {
            return null;
        }

        SupplierPaymentDto dto = new SupplierPaymentDto();
        dto.setSupplierPaymentId(entity.getSupplierPaymentId());
        if (entity.getPurchase() != null) {
            dto.setPurchaseId(entity.getPurchase().getPurchaseId());
        }
        dto.setPaymentDate(entity.getPaymentDate());
        dto.setPaymentMode(entity.getPaymentMode());
        dto.setReferenceNumber(entity.getReferenceNumber());
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setOutstandingAmount(entity.getOutstandingAmount());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    // DTO -> Entity (purchase association is set by the service)
    public SupplierPayment toEntity(SupplierPaymentDto dto) {
        if (dto == null) {
            return null;
        }

        SupplierPayment entity = new SupplierPayment();
        entity.setSupplierPaymentId(dto.getSupplierPaymentId());
        entity.setPaymentDate(dto.getPaymentDate());
        entity.setPaymentMode(dto.getPaymentMode());
        entity.setReferenceNumber(dto.getReferenceNumber());
        entity.setPaidAmount(dto.getPaidAmount());
        entity.setOutstandingAmount(dto.getOutstandingAmount());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        return entity;
    }
}
