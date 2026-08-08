package tiameds.pharmabackend.mapper.billing;

import tiameds.pharmabackend.dto.billing.BillingPaymentDto;
import tiameds.pharmabackend.entity.billing.BillingPayment;

public class BillingPaymentMapper {

    public static BillingPaymentDto toDto(BillingPayment entity) {

        if (entity == null) {
            return null;
        }

        BillingPaymentDto dto = new BillingPaymentDto();

        dto.setPaymentId(entity.getPaymentId());
        dto.setBillingId(
                entity.getBilling() != null
                        ? entity.getBilling().getBillingId()
                        : null
        );
        dto.setPaymentMode(entity.getPaymentMode());
        dto.setTransactionId(entity.getTransactionId());
        dto.setReceivedAmount(entity.getReceivedAmount());
        dto.setPendingAmount(entity.getPendingAmount());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        return dto;
    }

    public static BillingPayment toEntity(BillingPaymentDto dto) {

        if (dto == null) {
            return null;
        }

        BillingPayment entity = new BillingPayment();

        entity.setPaymentId(dto.getPaymentId());
        entity.setPaymentMode(dto.getPaymentMode());
        entity.setTransactionId(dto.getTransactionId());
        entity.setReceivedAmount(dto.getReceivedAmount());
        entity.setPendingAmount(dto.getPendingAmount());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        return entity;
    }
}
