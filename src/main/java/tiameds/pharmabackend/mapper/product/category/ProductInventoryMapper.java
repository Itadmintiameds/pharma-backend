package tiameds.pharmabackend.mapper.product.category;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tiameds.pharmabackend.dto.product.BatchDetailsDto;
import tiameds.pharmabackend.dto.product.PackagingDetailsDto;
import tiameds.pharmabackend.entity.master.PurchaseSmallestUnit;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.repository.master.PurchaseSmallestUnitRepository;

@Component
public class ProductInventoryMapper {

    @Autowired
    private PurchaseSmallestUnitRepository purchaseSmallestUnitRepository;

    /**
     * Resolves the PurchaseSmallestUnit master by id and links it to the packaging entity.
     * The packaging's purchaseUnit string is always derived from the master's purchaseUnitName.
     */
    public void applyPurchaseSmallestUnit(PackagingDetails entity, Long purchaseSmallestUnitId) {
        if (purchaseSmallestUnitId == null) return;
        PurchaseSmallestUnit master = purchaseSmallestUnitRepository.findById(purchaseSmallestUnitId)
                .orElseThrow(() -> new RuntimeException(
                        "Purchase smallest unit not found with id: " + purchaseSmallestUnitId));
        entity.setPurchaseSmallestUnit(master);
        entity.setPurchaseUnit(master.getPurchaseUnitName());
    }

    public BatchDetails toEntity(BatchDetailsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        BatchDetails entity = new BatchDetails();
        if (dto.getPackagingId() != null) {
            PackagingDetails pd = new PackagingDetails();
            pd.setPackagingId(dto.getPackagingId());
            entity.setPackagingDetails(pd);
        }
        entity.setBatchNumber(dto.getBatchNumber());
        entity.setManufacturingDate(dto.getManufacturingDate());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setPurchaseUnit(dto.getPurchaseUnit());

        entity.setPurchasePrice(dto.getPurchasePrice());
        entity.setMrp(dto.getMrp());
        entity.setSellingPrice(dto.getSellingPrice());
        entity.setPurchasePricePerUnit(dto.getPurchasePricePerUnit());
        entity.setMrpPerUnit(dto.getMrpPerUnit());
        entity.setSellingPricePerUnit(dto.getSellingPricePerUnit());
        entity.setRackLocation(dto.getRackLocation());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    public PackagingDetails toEntity(PackagingDetailsDto dto, String createdBy, LocalDateTime createdAt) {
        if (dto == null) return null;
        PackagingDetails entity = new PackagingDetails();
        // purchaseUnit is derived from the linked PurchaseSmallestUnit master (see applyPurchaseSmallestUnit)
        // entity.setPurchaseUnit(dto.getPurchaseUnit());
        entity.setPurchaseUnitContains(dto.getPurchaseUnitContains());
        // entity.setSmallestUnit(dto.getSmallestUnit());
        applyPurchaseSmallestUnit(entity, dto.getPurchaseSmallestUnitId());
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    public BatchDetailsDto toDto(BatchDetails entity) {
        if (entity == null) return null;
        BatchDetailsDto dto = new BatchDetailsDto();
        dto.setBatchId(entity.getBatchId());
        if (entity.getPackagingDetails() != null) {
            dto.setPackagingId(entity.getPackagingDetails().getPackagingId());
        }
        dto.setBatchNumber(entity.getBatchNumber());
        dto.setManufacturingDate(entity.getManufacturingDate());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setPurchaseUnit(entity.getPurchaseUnit());

        dto.setPurchasePrice(entity.getPurchasePrice());
        dto.setMrp(entity.getMrp());
        dto.setSellingPrice(entity.getSellingPrice());
        dto.setPurchasePricePerUnit(entity.getPurchasePricePerUnit());
        dto.setMrpPerUnit(entity.getMrpPerUnit());
        dto.setSellingPricePerUnit(entity.getSellingPricePerUnit());
        dto.setRackLocation(entity.getRackLocation());
        return dto;
    }

    public PackagingDetailsDto toDto(PackagingDetails entity) {
        if (entity == null) return null;
        PackagingDetailsDto dto = new PackagingDetailsDto();
        dto.setPackagingId(entity.getPackagingId());
        dto.setPurchaseUnit(entity.getPurchaseUnit());
        dto.setPurchaseUnitContains(entity.getPurchaseUnitContains());
        // dto.setSmallestUnit(entity.getSmallestUnit());
        if (entity.getPurchaseSmallestUnit() != null) {
            dto.setPurchaseSmallestUnitId(entity.getPurchaseSmallestUnit().getPurchaseSmallestUnitId());
            dto.setPurchaseSmallestUnitName(entity.getPurchaseSmallestUnit().getPurchaseSmallestUnitName());
        }
        return dto;
    }

}
