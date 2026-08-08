package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.PurchaseSmallestUnitDto;

import java.util.List;

public interface PurchaseSmallestUnitService {

    List<PurchaseSmallestUnitDto> getAllPurchaseSmallestUnits();

    PurchaseSmallestUnitDto getPurchaseSmallestUnitById(Long purchaseSmallestUnitId);

    List<PurchaseSmallestUnitDto> getPurchaseSmallestUnitsByCategoryId(Long productCategoryId);

    PurchaseSmallestUnitDto createPurchaseSmallestUnit(PurchaseSmallestUnitDto purchaseSmallestUnitDto);

    PurchaseSmallestUnitDto updatePurchaseSmallestUnit(Long purchaseSmallestUnitId, PurchaseSmallestUnitDto purchaseSmallestUnitDto);

    PurchaseSmallestUnitDto updatePurchaseSmallestUnitStatus(Long purchaseSmallestUnitId, Boolean isActive);
}
