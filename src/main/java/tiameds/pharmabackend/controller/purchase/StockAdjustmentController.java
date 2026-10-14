package tiameds.pharmabackend.controller.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.purchase.StockAdjustmentDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.purchase.StockAdjustmentService;

import java.util.List;

@RestController
@RequestMapping("/stock-adjustment")
@RequiredArgsConstructor
public class StockAdjustmentController {

    private final StockAdjustmentService stockAdjustmentService;

    // Sets the batch's stock at the caller's current warehouse or pharmacy to
    // actualStockQty. adjustmentType (INCREASE / DECREASE) must match the direction.
    @PreAuthorize("@access.has('STOCK_ADJUSTMENT/STOCK_ADJUSTMENT/CREATE')")
    @PostMapping("/create")
    public ResponseEntity<?> createStockAdjustment(
            @RequestBody StockAdjustmentDto stockAdjustmentDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        StockAdjustmentDto response = stockAdjustmentService.createStockAdjustment(
                stockAdjustmentDto,
                currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PreAuthorize("@access.has('STOCK_ADJUSTMENT/STOCK_ADJUSTMENT/VIEW')")
    @GetMapping("/allStockAdjustment")
    public ResponseEntity<?> getAllStockAdjustments(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<StockAdjustmentDto> stockAdjustments =
                stockAdjustmentService.getAllStockAdjustments(currentUser.getUser());

        return ResponseEntity.ok(stockAdjustments);
    }


    // KPI cards for the stock adjustment screen: number of adjustments and
    // total adjustment value, for the current warehouse or pharmacy.
    @PreAuthorize("@access.has('STOCK_ADJUSTMENT/STOCK_ADJUSTMENT/VIEW')")
    @GetMapping("/kpi")
    public ResponseEntity<?> getStockAdjustmentKpis(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(stockAdjustmentService.getStockAdjustmentKpis(currentUser.getUser()));
    }


    @PreAuthorize("@access.has('STOCK_ADJUSTMENT/STOCK_ADJUSTMENT/VIEW')")
    @GetMapping(" {stockAdjustmentId}")
    public ResponseEntity<?> getStockAdjustmentById(
            @PathVariable Long stockAdjustmentId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        StockAdjustmentDto stockAdjustment = stockAdjustmentService.getStockAdjustmentById(
                stockAdjustmentId,
                currentUser.getUser());

        return ResponseEntity.ok(stockAdjustment);
    }
}
