package tiameds.pharmabackend.mapper.billing;

import tiameds.pharmabackend.dto.billing.BillingDto;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingDetails;
import tiameds.pharmabackend.entity.billing.BillingPayment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BillingMapper {

    public static BillingDto toDto(Billing entity) {

        if (entity == null) {
            return null;
        }

        BillingDto dto = new BillingDto();

        dto.setBillingId(entity.getBillingId());
        dto.setBillNo(entity.getBillNo());
        dto.setPharmacyId(
                entity.getPharmacy() != null
                        ? entity.getPharmacy().getPharmacyId()
                        : null
        );

        if (entity.getCustomer() != null) {
            dto.setCustomerId(entity.getCustomer().getCustomerId());
            dto.setCustomerName(entity.getCustomer().getCustomerName());
            dto.setCustomerPhoneNo(entity.getCustomer().getCustomerPhoneNo());
            dto.setCustomerAddress(entity.getCustomer().getCustomerAddress());
        }

        if (entity.getDoctor() != null) {
            dto.setDoctorId(entity.getDoctor().getDoctorId());
            dto.setDoctorName(entity.getDoctor().getDoctorName());
        }

        dto.setCustomerType(entity.getCustomerType());
        dto.setPrescriptionUrl(entity.getPrescriptionUrl());
        dto.setTotalDiscountPercentage(entity.getTotalDiscountPercentage());
        dto.setTotalDiscountAmount(entity.getTotalDiscountAmount());
        dto.setTotalGstAmount(entity.getTotalGstAmount());
        dto.setTotalGrossAmount(entity.getTotalGrossAmount());
        dto.setTotalNetAmount(entity.getTotalNetAmount());
        dto.setSellingType(entity.getSellingType());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        if (entity.getBillingDetails() != null) {
            dto.setBillingDetails(
                    entity.getBillingDetails()
                            .stream()
                            .map(BillingDetailsMapper::toDto)
                            .collect(Collectors.toList())
            );
        }

        if (entity.getBillingPayments() != null) {
            dto.setBillingPayments(
                    entity.getBillingPayments()
                            .stream()
                            .map(BillingPaymentMapper::toDto)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    public static Billing toEntity(BillingDto dto) {

        if (dto == null) {
            return null;
        }

        Billing entity = new Billing();

        entity.setBillingId(dto.getBillingId());

        // pharmacy and customer are resolved and attached by the service

        entity.setCustomerType(dto.getCustomerType());
        entity.setPrescriptionUrl(dto.getPrescriptionUrl());
        entity.setTotalDiscountPercentage(dto.getTotalDiscountPercentage());
        entity.setTotalDiscountAmount(dto.getTotalDiscountAmount());
        entity.setTotalGstAmount(dto.getTotalGstAmount());
        entity.setTotalGrossAmount(dto.getTotalGrossAmount());
        entity.setTotalNetAmount(dto.getTotalNetAmount());
        entity.setSellingType(dto.getSellingType());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        if (dto.getBillingDetails() != null) {

            List<BillingDetails> details = new ArrayList<>();

            for (var detailsDto : dto.getBillingDetails()) {
                BillingDetails billingDetails = BillingDetailsMapper.toEntity(detailsDto);
                billingDetails.setBilling(entity);
                details.add(billingDetails);
            }

            entity.setBillingDetails(details);
        }

        if (dto.getBillingPayments() != null) {

            List<BillingPayment> payments = new ArrayList<>();

            for (var paymentDto : dto.getBillingPayments()) {
                BillingPayment billingPayment = BillingPaymentMapper.toEntity(paymentDto);
                billingPayment.setBilling(entity);
                payments.add(billingPayment);
            }

            entity.setBillingPayments(payments);
        }

        return entity;
    }
}
