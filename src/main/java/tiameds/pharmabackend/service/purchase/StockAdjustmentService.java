package tiameds.pharmabackend.service.purchase;

import tiameds.pharmabackend.dto.purchase.StockAdjustmentDto;
import tiameds.pharmabackend.dto.purchase.StockAdjustmentKpiResponse;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface StockAdjustmentService {

    // Adds or removes stock (per adjustmentType) at the caller's current
    // location — warehouse or pharmacy — and records the adjustment.
    StockAdjustmentDto createStockAdjustment(StockAdjustmentDto stockAdjustmentDto, UserDetails user);

    // Adjustments raised at the caller's current location, newest first.
    List<StockAdjustmentDto> getAllStockAdjustments(UserDetails user);

    // Only an adjustment raised at the caller's current location is returned.
    StockAdjustmentDto getStockAdjustmentById(Long stockAdjustmentId, UserDetails user);

    StockAdjustmentKpiResponse getStockAdjustmentKpis(UserDetails user);

}
