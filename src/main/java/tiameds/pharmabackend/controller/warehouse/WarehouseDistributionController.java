package tiameds.pharmabackend.controller.warehouse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionDispatchRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionReceiveRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionRequest;
import tiameds.pharmabackend.dto.warehouse.WarehouseDistributionResponse;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.warehouse.WarehouseDistributionService;

import java.util.Map;

@RestController
@RequestMapping("/warehouse/distribution")
@RequiredArgsConstructor
public class WarehouseDistributionController {

    private final WarehouseDistributionService distributionService;

    // Next allocation number to display on a blank create form (preview only)
    @PreAuthorize("@access.has('WAREHOUSE_DISTRIBUTION/WAREHOUSE_DISTRIBUTION/CREATE')")
    @GetMapping("/next-allocation-no")
    public ResponseEntity<?> nextAllocationNo(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(Map.of("allocationNo",
                distributionService.peekNextAllocationNo(currentUser.getUser())));
    }

    // Create an allocation (DISTRIBUTION_CREATED)
    @PreAuthorize("@access.has('WAREHOUSE_DISTRIBUTION/WAREHOUSE_DISTRIBUTION/CREATE')")
    @PostMapping("/create")
    public ResponseEntity<?> createAllocation(
            @RequestBody WarehouseDistributionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WarehouseDistributionResponse response =
                distributionService.createAllocation(request, currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Dispatch: source stock leaves (PRODUCTS_DISPATCHED). The optional body carries
    // the quantities actually shipped (which may be less than issued) and a per-line
    // remark; omit it to dispatch every line at its issued quantity.
    @PreAuthorize("@access.hasAny(" +
            "'WAREHOUSE_DISTRIBUTION/WAREHOUSE_DISTRIBUTION/CREATE', " +
            "'INTER_STORE_TRANSFER/INTER_STORE_TRANSFER/CREATE')")
    @PostMapping("/{distributionId}/dispatch")
    public ResponseEntity<?> dispatch(
            @PathVariable Long distributionId,
            @RequestBody(required = false) WarehouseDistributionDispatchRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        distributionService.dispatch(distributionId, request, currentUser.getUser());

        return ResponseEntity.ok(distributionService.getById(distributionId));
    }

    // Receive: destination stock arrives (STOCK_RECEIVED). The optional body carries
    // the products/quantities that actually arrived; omit it to receive every line at
    // its issued quantity.
    @PreAuthorize("@access.hasAny('WAREHOUSE_RECEIPT/WAREHOUSE_RECEIPT/CREATE', " +
            "'WAREHOUSE_DISTRIBUTION/WAREHOUSE_DISTRIBUTION/CREATE')")
    @PostMapping("/{distributionId}/receive")
    public ResponseEntity<?> receive(
            @PathVariable Long distributionId,
            @RequestBody(required = false) WarehouseDistributionReceiveRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        distributionService.receive(distributionId, request, currentUser.getUser());

        return ResponseEntity.ok(distributionService.getById(distributionId));
    }

    // List this warehouse's distributions (incoming + outgoing) as summary rows
    @PreAuthorize("@access.has('WAREHOUSE_DISTRIBUTION/WAREHOUSE_DISTRIBUTION/VIEW')")
    @GetMapping("/warehouse/list")
    public ResponseEntity<?> getAll(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(distributionService.getAll(currentUser.getUser()));
    }

    // Distributions shipped FROM this warehouse (it is the source)
    @PreAuthorize("@access.has('INTER_STORE_TRANSFER/INTER_STORE_TRANSFER/VIEW')")
    @GetMapping("/warehouse/source")
    public ResponseEntity<?> getBySource(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(distributionService.getBySource(currentUser.getUser()));
    }

    // Distributions shipped TO this warehouse (it is the destination)
    @PreAuthorize("@access.has('WAREHOUSE_RECEIPT/WAREHOUSE_RECEIPT/VIEW')")
    @GetMapping("/warehouse/destination")
    public ResponseEntity<?> getByDestination(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(distributionService.getByDestination(currentUser.getUser()));
    }

    // KPI cards for the destination screen: pending receipts (PRODUCTS_DISPATCHED),
    // received today, and products received today — all for this warehouse/pharmacy
    // as the destination.
    @PreAuthorize("@access.has('WAREHOUSE_RECEIPT/WAREHOUSE_RECEIPT/VIEW')")
    @GetMapping("/warehouse/destination/kpi")
    public ResponseEntity<?> getDestinationKpis(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(distributionService.getDestinationKpis(currentUser.getUser()));
    }

    // KPI cards for the source screen: ready to dispatch (DISTRIBUTION_CREATED),
    // pending receipt (PRODUCTS_DISPATCHED) and completed (STOCK_RECEIVED) — all for
    // this warehouse/pharmacy as the source.
    @PreAuthorize("@access.has('INTER_STORE_TRANSFER/INTER_STORE_TRANSFER/VIEW')")
    @GetMapping("/warehouse/source/kpi")
    public ResponseEntity<?> getSourceKpis(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(distributionService.getSourceKpis(currentUser.getUser()));
    }

    // KPI cards for the "my requests" screen, scoped to the acting warehouse as the
    // requesting warehouse: total transfers (all time), completed (STOCK_RECEIVED),
    // pending (PRODUCTS_DISPATCHED) and ready to dispatch (DISTRIBUTION_CREATED).
    @PreAuthorize("@access.has('WAREHOUSE_DISTRIBUTION/WAREHOUSE_DISTRIBUTION/VIEW')")
    @GetMapping("/warehouse/requested-by/kpi")
    public ResponseEntity<?> getRequestedByKpis(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(distributionService.getRequestedByKpis(currentUser.getUser()));
    }

    // Read one distribution with its lines and current status
    @PreAuthorize("@access.hasAny('WAREHOUSE_DISTRIBUTION/WAREHOUSE_DISTRIBUTION/VIEW', " +
            "'WAREHOUSE_RECEIPT/WAREHOUSE_RECEIPT/VIEW', " +
            "'INTER_STORE_TRANSFER/INTER_STORE_TRANSFER/VIEW')")
    @GetMapping("/{distributionId}")
    public ResponseEntity<WarehouseDistributionResponse> getById(
            @PathVariable Long distributionId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(distributionService.getById(distributionId));
    }
}
