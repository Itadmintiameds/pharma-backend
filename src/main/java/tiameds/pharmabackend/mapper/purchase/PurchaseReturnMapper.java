package tiameds.pharmabackend.mapper.purchase;

import tiameds.pharmabackend.dto.purchase.PurchaseReturnDetailsDto;
import tiameds.pharmabackend.dto.purchase.PurchaseReturnDto;
import tiameds.pharmabackend.entity.purchase.Purchase;
import tiameds.pharmabackend.entity.purchase.PurchaseReturn;
import tiameds.pharmabackend.entity.purchase.PurchaseReturnDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PurchaseReturnMapper {

    public static PurchaseReturnDto toDto(PurchaseReturn entity) {

        if (entity == null) {
            return null;
        }

        PurchaseReturnDto dto = new PurchaseReturnDto();

        dto.setPurchaseReturnId(entity.getPurchaseReturnId());
        dto.setReturnNo(entity.getReturnNo());
        dto.setPurchaseReturnDate(entity.getPurchaseReturnDate());

        Purchase purchase = entity.getPurchase();

        if (purchase != null) {

            dto.setPurchaseId(purchase.getPurchaseId());
            dto.setGrnNo(purchase.getGrnNo());
            dto.setInvoiceNo(purchase.getInvoiceNo());
            dto.setInvoiceDate(purchase.getInvoiceDate());

            if (purchase.getSupplier() != null) {
                dto.setSupplierId(purchase.getSupplier().getSupplierId());
                dto.setSupplierName(purchase.getSupplier().getSupplierName());
            }
        }

        dto.setPharmacyId(entity.getPharmacyId());
        dto.setWarehouseId(entity.getWarehouseId());
        dto.setStatus(entity.getStatus());
        dto.setCancelReason(entity.getCancelReason());
        dto.setEditReason(entity.getEditReason());
        dto.setTotalGrossAmount(entity.getTotalGrossAmount());
        dto.setTotalGstAmount(entity.getTotalGstAmount());
        dto.setTotalNetAmount(entity.getTotalNetAmount());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setModifiedBy(entity.getModifiedBy());
        dto.setModifiedAt(entity.getModifiedAt());

        if (entity.getPurchaseReturnDetails() != null) {

            // Only the latest revision of each line is part of the return;
            // superseded rows are kept for history but not shown or counted.
            List<PurchaseReturnDetailsDto> currentDetails = entity.getPurchaseReturnDetails()
                    .stream()
                    .filter(PurchaseReturnDetails::isCurrent)
                    .map(PurchaseReturnDetailsMapper::toDto)
                    .collect(Collectors.toList());

            dto.setPurchaseReturnDetails(currentDetails);

            dto.setItemCount(currentDetails.size());

        } else {
            dto.setItemCount(0);
        }

        return dto;
    }

    public static PurchaseReturn toEntity(PurchaseReturnDto dto) {
        if (dto == null) {
            return null;
        }

        PurchaseReturn entity = new PurchaseReturn();

        entity.setPurchaseReturnId(dto.getPurchaseReturnId());

        if (dto.getPurchaseId() != null) {
            Purchase purchase = new Purchase();
            purchase.setPurchaseId(dto.getPurchaseId());
            entity.setPurchase(purchase);
        }

        entity.setPharmacyId(dto.getPharmacyId());
        entity.setWarehouseId(dto.getWarehouseId());
        entity.setReturnNo(dto.getReturnNo());
        entity.setPurchaseReturnDate(dto.getPurchaseReturnDate());
        entity.setStatus(dto.getStatus());
        entity.setCancelReason(dto.getCancelReason());
        entity.setEditReason(dto.getEditReason());
        entity.setTotalGrossAmount(dto.getTotalGrossAmount());
        entity.setTotalGstAmount(dto.getTotalGstAmount());
        entity.setTotalNetAmount(dto.getTotalNetAmount());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setModifiedBy(dto.getModifiedBy());
        entity.setModifiedAt(dto.getModifiedAt());

        if (dto.getPurchaseReturnDetails() != null) {

            List<PurchaseReturnDetails> details = new ArrayList<>();

            for (var detailsDto : dto.getPurchaseReturnDetails()) {
                PurchaseReturnDetails purchaseReturnDetails = PurchaseReturnDetailsMapper.toEntity(detailsDto);
                purchaseReturnDetails.setPurchaseReturn(entity);
                details.add(purchaseReturnDetails);
            }

            entity.setPurchaseReturnDetails(details);
        }

        return entity;
    }
}
