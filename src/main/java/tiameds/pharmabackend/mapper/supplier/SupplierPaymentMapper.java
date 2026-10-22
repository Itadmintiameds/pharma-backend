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
        if (entity.getSupplierMaster() != null) {
            dto.setSupplierId(entity.getSupplierMaster().getSupplierId());
        }
        if (entity.getPurchase() != null) {
            dto.setPurchaseId(entity.getPurchase().getPurchaseId());
        }
        dto.setSupplierPaymentNo(entity.getSupplierPaymentNo());
        dto.setSupplierPaymentDate(entity.getSupplierPaymentDate());
        dto.setPaymentMode(entity.getPaymentMode());
        dto.setPaymentAmount(entity.getPaymentAmount());
        dto.setTransactionNo(entity.getTransactionNo());
        dto.setRemarks(entity.getRemarks());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    // DTO -> Entity (supplier and purchase associations are set by the service)
    public SupplierPayment toEntity(SupplierPaymentDto dto) {
        if (dto == null) {
            return null;
        }

        SupplierPayment entity = new SupplierPayment();
        entity.setSupplierPaymentId(dto.getSupplierPaymentId());
        entity.setSupplierPaymentNo(dto.getSupplierPaymentNo());
        entity.setSupplierPaymentDate(dto.getSupplierPaymentDate());
        entity.setPaymentMode(dto.getPaymentMode());
        entity.setPaymentAmount(dto.getPaymentAmount());
        entity.setTransactionNo(dto.getTransactionNo());
        entity.setRemarks(dto.getRemarks());

        return entity;
    }
}
