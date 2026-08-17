package tiameds.pharmabackend.mapper.purchase;

import tiameds.pharmabackend.dto.purchase.PurchaseDto;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.entity.purchase.PurchaseDetails;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PurchaseMapper {

    public static PurchaseDto toDto(Purchase entity) {

        if (entity == null) {
            return null;
        }

        PurchaseDto dto = new PurchaseDto();

        dto.setPurchaseId(entity.getPurchaseId());
        dto.setPharmacyId(entity.getPharmacyId());
        dto.setWarehouseId(entity.getWarehouseId());
        dto.setSupplierId(entity.getSupplier().getSupplierId());
        dto.setGrnNo(entity.getGrnNo());
        dto.setInvoiceNo(entity.getInvoiceNo());
        dto.setInvoiceDate(entity.getInvoiceDate());
        dto.setPaymentType(entity.getPaymentType());
        dto.setCreditDays(entity.getCreditDays());
        dto.setSupplierPaymentStatus(entity.getSupplierPaymentStatus());
        dto.setTotalGrossAmount(entity.getTotalGrossAmount());
        dto.setTotalDiscount(entity.getTotalDiscount());
        dto.setTotalGst(entity.getTotalGst());
        dto.setTotalNetAmount(entity.getTotalNetAmount());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        if (entity.getPurchaseDetails() != null) {
            dto.setPurchaseDetails(
                    entity.getPurchaseDetails()
                            .stream()
                            .map(PurchaseDetailsMapper::toDto)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }


    public static Purchase toEntity(PurchaseDto dto) {
        if (dto == null) {
            return null;
        }

        Purchase entity = new Purchase();

        entity.setPurchaseId(dto.getPurchaseId());
        entity.setPharmacyId(dto.getPharmacyId());

        if (dto.getSupplierId() != null) {
            SupplierMaster supplier = new SupplierMaster();
            supplier.setSupplierId(dto.getSupplierId());
            entity.setSupplier(supplier);
        }

        entity.setGrnNo(dto.getGrnNo());
        entity.setInvoiceNo(dto.getInvoiceNo());
        entity.setInvoiceDate(dto.getInvoiceDate());
        entity.setPaymentType(dto.getPaymentType());
        entity.setCreditDays(dto.getCreditDays());
        entity.setSupplierPaymentStatus(dto.getSupplierPaymentStatus());
        entity.setTotalGrossAmount(dto.getTotalGrossAmount());
        entity.setTotalDiscount(dto.getTotalDiscount());
        entity.setTotalGst(dto.getTotalGst());
        entity.setTotalNetAmount(dto.getTotalNetAmount());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        if (dto.getPurchaseDetails() != null) {

            List<PurchaseDetails> details = new ArrayList<>();

            for (var detailsDto : dto.getPurchaseDetails()) {
                PurchaseDetails purchaseDetails = PurchaseDetailsMapper.toEntity(detailsDto);
                purchaseDetails.setPurchase(entity);
                details.add(purchaseDetails);
            }

            entity.setPurchaseDetails(details);
        }

        return entity;
    }
}