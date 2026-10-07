package tiameds.pharmabackend.mapper.billing;

import tiameds.pharmabackend.dto.billing.SalesReturnDetailsDto;
import tiameds.pharmabackend.dto.billing.SalesReturnDto;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.SalesReturn;
import tiameds.pharmabackend.enums.BillReturnStatus;

import java.util.List;
import java.util.stream.Collectors;

public class SalesReturnMapper {

    public static SalesReturnDto toDto(SalesReturn entity) {

        if (entity == null) {
            return null;
        }

        SalesReturnDto dto = new SalesReturnDto();

        dto.setSalesReturnId(entity.getSalesReturnId());

        Billing billing = entity.getBilling();

        if (billing != null) {
            dto.setBillingId(billing.getBillingId());
            dto.setBillNo(billing.getBillNo());

            // Bills raised before the column existed carry a null.
            dto.setBillReturnStatus(billing.getSalesReturnStatus() != null
                    ? billing.getSalesReturnStatus()
                    : BillReturnStatus.NOT_RETURNED);

            dto.setCustomerType(billing.getCustomerType());

            if (billing.getCustomer() != null) {
                dto.setCustomerId(billing.getCustomer().getCustomerId());
                dto.setCustomerName(billing.getCustomer().getCustomerName());
                dto.setCustomerPhoneNo(billing.getCustomer().getCustomerPhoneNo());
            }
        }

        dto.setPharmacyId(entity.getPharmacyId());
        dto.setSalesReturnNo(entity.getSalesReturnNo());
        dto.setSalesReturnDate(entity.getSalesReturnDate());
        dto.setSalesReturnStatus(entity.getSalesReturnStatus());
        dto.setTotalGrossAmount(entity.getTotalGrossAmount());
        dto.setTotalGstAmount(entity.getTotalGstAmount());
        dto.setTotalNetAmount(entity.getTotalNetAmount());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        if (entity.getSalesReturnDetails() != null) {

            List<SalesReturnDetailsDto> details = entity.getSalesReturnDetails()
                    .stream()
                    .map(SalesReturnDetailsMapper::toDto)
                    .collect(Collectors.toList());

            dto.setSalesReturnDetails(details);
            dto.setItemCount(details.size());

        } else {
            dto.setItemCount(0);
        }

        return dto;
    }
}
