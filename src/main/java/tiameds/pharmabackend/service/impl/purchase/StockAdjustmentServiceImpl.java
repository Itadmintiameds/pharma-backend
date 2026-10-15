package tiameds.pharmabackend.service.impl.purchase;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.purchase.StockAdjustmentDto;
import tiameds.pharmabackend.dto.purchase.StockAdjustmentKpiResponse;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.purchase.Inventory;
import tiameds.pharmabackend.entity.purchase.StockAdjustment;
import tiameds.pharmabackend.entity.warehouse.WarehouseInventory;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.enums.TransactionType;
import tiameds.pharmabackend.mapper.purchase.StockAdjustmentMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.purchase.InventoryRepository;
import tiameds.pharmabackend.repository.purchase.StockAdjustmentRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseInventoryRepository;
import tiameds.pharmabackend.service.impl.warehouse.stock.InventoryAdjusters;
import tiameds.pharmabackend.service.purchase.StockAdjustmentService;
import tiameds.pharmabackend.service.warehouse.stock.InventoryAdjuster;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StockAdjustmentServiceImpl implements StockAdjustmentService {

    private static final String ADJUSTMENT_INCREASE = "INCREASE";
    private static final String ADJUSTMENT_DECREASE = "DECREASE";

    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final ProductDetailsRepository pharmaProductDetailsRepository;
    private final BatchDetailsRepository pharmaBatchDetailsRepository;
    private final LocationContextResolver locationContextResolver;
    private final InventoryAdjusters adjusters;
    private final InventoryRepository inventoryRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;

    @Override
    public StockAdjustmentDto createStockAdjustment(StockAdjustmentDto stockAdjustmentDto, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Same decision point as a purchase return: a warehouse manager adjusts
        // their warehouse's stock, everyone else their selected pharmacy's.
        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isWarehouse()) {
            return createWarehouseStockAdjustment(stockAdjustmentDto, persistentUser, location.getLocationId());
        }

        return createPharmacyStockAdjustment(stockAdjustmentDto, persistentUser, location.getLocationId());
    }

    @Override
    public List<StockAdjustmentDto> getAllStockAdjustments(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isWarehouse()) {
            return stockAdjustmentRepository.findByWarehouseId(location.getLocationId())
                    .stream()
                    .map(StockAdjustmentMapper::toDto)
                    .collect(Collectors.toList());
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return stockAdjustmentRepository.findByPharmacyId(pharmacyId)
                .stream()
                .map(StockAdjustmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public StockAdjustmentDto getStockAdjustmentById(Long stockAdjustmentId, UserDetails user) {

        if (stockAdjustmentId == null) {
            throw new RuntimeException("Stock adjustment id is required");
        }

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isWarehouse()) {

            StockAdjustment stockAdjustment = stockAdjustmentRepository
                    .findByStockAdjustmentIdAndWarehouseId(stockAdjustmentId, location.getLocationId())
                    .orElseThrow(() -> new RuntimeException(
                            "Stock adjustment not found in this warehouse with id : " + stockAdjustmentId));

            return StockAdjustmentMapper.toDto(stockAdjustment);
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        StockAdjustment stockAdjustment = stockAdjustmentRepository
                .findByStockAdjustmentIdAndPharmacyId(stockAdjustmentId, pharmacyId)
                .orElseThrow(() -> new RuntimeException(
                        "Stock adjustment not found in this pharmacy with id : " + stockAdjustmentId));

        return StockAdjustmentMapper.toDto(stockAdjustment);
    }

    @Override
    public StockAdjustmentKpiResponse getStockAdjustmentKpis(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        StockAdjustmentKpiResponse response = new StockAdjustmentKpiResponse();

        if (location.isWarehouse()) {

            String warehouseId = location.getLocationId();

            response.setTotalStockAdjustments(stockAdjustmentRepository.countByWarehouseId(warehouseId));
            response.setTotalStockValue(stockAdjustmentRepository.sumAdjustmentValueByWarehouseId(warehouseId));

            return response;
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        response.setTotalStockAdjustments(stockAdjustmentRepository.countByPharmacyId(pharmacyId));
        response.setTotalStockValue(stockAdjustmentRepository.sumAdjustmentValueByPharmacyId(pharmacyId));

        return response;
    }

    private StockAdjustmentDto createPharmacyStockAdjustment(
            StockAdjustmentDto stockAdjustmentDto,
            UserDetails persistentUser,
            String pharmacyId) {

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        String adjustmentType = resolveAdjustmentType(stockAdjustmentDto.getAdjustmentType());
        validateActualStockQty(stockAdjustmentDto.getActualStockQty());

        StockAdjustment stockAdjustment = StockAdjustmentMapper.toEntity(stockAdjustmentDto);

        stockAdjustment.setPharmacyId(pharmacyId);
        stockAdjustment.setWarehouseId(null);
        stockAdjustment.setAdjustmentType(adjustmentType);
        stockAdjustment.setStockAdjustmentNo(generateStockAdjustmentNo(pharmacyId));

        resolveProductAndBatch(stockAdjustment, stockAdjustmentDto);

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        stampAuditFields(stockAdjustment, actor, now);

        adjustInventory(stockAdjustment, LocationType.PHARMACY, pharmacyId, actor, now);

        StockAdjustment savedStockAdjustment = stockAdjustmentRepository.save(stockAdjustment);

        return StockAdjustmentMapper.toDto(savedStockAdjustment);
    }

    private StockAdjustmentDto createWarehouseStockAdjustment(
            StockAdjustmentDto stockAdjustmentDto,
            UserDetails persistentUser,
            String warehouseId) {

        String adjustmentType = resolveAdjustmentType(stockAdjustmentDto.getAdjustmentType());
        validateActualStockQty(stockAdjustmentDto.getActualStockQty());

        StockAdjustment stockAdjustment = StockAdjustmentMapper.toEntity(stockAdjustmentDto);

        stockAdjustment.setPharmacyId(null);
        stockAdjustment.setWarehouseId(warehouseId);
        stockAdjustment.setAdjustmentType(adjustmentType);
        stockAdjustment.setStockAdjustmentNo(generateStockAdjustmentNoForWarehouse(warehouseId));

        resolveProductAndBatch(stockAdjustment, stockAdjustmentDto);

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        stampAuditFields(stockAdjustment, actor, now);

        adjustInventory(stockAdjustment, LocationType.WAREHOUSE, warehouseId, actor, now);

        StockAdjustment savedStockAdjustment = stockAdjustmentRepository.save(stockAdjustment);

        return StockAdjustmentMapper.toDto(savedStockAdjustment);
    }

    // adjustmentType is stored as sent (upper-cased); only INCREASE and DECREASE are
    // understood, so anything else is rejected before stock moves.
    private String resolveAdjustmentType(String requested) {

        if (requested == null || requested.isBlank()) {
            throw new RuntimeException("Adjustment type is required");
        }

        String type = requested.trim().toUpperCase();

        if (!ADJUSTMENT_INCREASE.equals(type) && !ADJUSTMENT_DECREASE.equals(type)) {
            throw new RuntimeException(
                    "Invalid adjustment type: " + requested + ". Expected INCREASE or DECREASE.");
        }

        return type;
    }

    private void validateActualStockQty(Long actualStockQty) {

        // Zero is valid: it writes the batch's stock off entirely.
        if (actualStockQty == null || actualStockQty < 0) {
            throw new RuntimeException("Actual stock quantity is required and cannot be negative");
        }
    }

    // Resolves the managed product/batch, matching the pattern
    // PurchaseReturnServiceImpl uses for its return lines.
    private void resolveProductAndBatch(StockAdjustment stockAdjustment, StockAdjustmentDto stockAdjustmentDto) {

        if (stockAdjustmentDto.getProductId() == null) {
            throw new RuntimeException("Product id is required");
        }

        if (stockAdjustmentDto.getBatchId() == null) {
            throw new RuntimeException("Batch id is required");
        }

        ProductDetails product = pharmaProductDetailsRepository
                .findById(stockAdjustmentDto.getProductId())
                .orElseThrow(() ->
                        new RuntimeException("Product not found: " + stockAdjustmentDto.getProductId()));

        BatchDetails batch = pharmaBatchDetailsRepository
                .findById(stockAdjustmentDto.getBatchId())
                .orElseThrow(() ->
                        new RuntimeException("Batch not found: " + stockAdjustmentDto.getBatchId()));

        stockAdjustment.setProduct(product);
        stockAdjustment.setBatch(batch);
    }

    // actualStockQty is the counted total stock, so the inventory is set to it
    // rather than added to or subtracted from. The difference from the current
    // stock is moved through the same InventoryAdjuster the purchase / purchase
    // return flows use — INCREASE as an IN movement, DECREASE as an OUT — so the
    // pharma_inventory_audit / pharma_warehouse_inventory_audit row records the
    // change and the new remaining stock, tagged STOCK_ADJUSTMENT.
    private void adjustInventory(
            StockAdjustment stockAdjustment,
            LocationType locationType,
            String locationId,
            String actor,
            LocalDateTime now) {

        BatchDetails batch = stockAdjustment.getBatch();
        PackagingDetails packaging = batch.getPackagingDetails();

        long currentStock = currentStock(
                locationType, locationId, stockAdjustment.getProduct(), packaging, batch);

        long delta = stockAdjustment.getActualStockQty() - currentStock;

        if (delta == 0) {
            throw new RuntimeException(
                    "Actual stock quantity is the same as the current stock (" + currentStock + "). Nothing to adjust.");
        }

        // The type has to agree with the numbers, so a stale screen can't
        // record an INCREASE that actually takes stock away (or vice versa).
        boolean increase = ADJUSTMENT_INCREASE.equals(stockAdjustment.getAdjustmentType());

        if (increase && delta < 0) {
            throw new RuntimeException("Adjustment type is INCREASE but actual stock ("
                    + stockAdjustment.getActualStockQty() + ") is lower than current stock (" + currentStock + ").");
        }

        if (!increase && delta > 0) {
            throw new RuntimeException("Adjustment type is DECREASE but actual stock ("
                    + stockAdjustment.getActualStockQty() + ") is higher than current stock (" + currentStock + ").");
        }

        // The adjusted quantity is derived, never taken from the request.
        stockAdjustment.setAdjustmentQty(Math.abs(delta));

        // The value carries the direction: positive for INCREASE, negative for
        // DECREASE, whatever sign the client sent — so summing values gives the
        // net change.
        if (stockAdjustment.getAdjustmentValue() != null) {
            long value = Math.abs(stockAdjustment.getAdjustmentValue());
            stockAdjustment.setAdjustmentValue(increase ? value : -value);
        }

        tiameds.pharmabackend.service.warehouse.stock.StockAdjustment adjustment =
                new tiameds.pharmabackend.service.warehouse.stock.StockAdjustment(
                        locationId,
                        stockAdjustment.getProduct(),
                        packaging,
                        batch,
                        Math.abs(delta),
                        TransactionType.STOCK_ADJUSTMENT,
                        null,   // no distribution line for a stock adjustment
                        null,   // no purchase line for a stock adjustment
                        actor,
                        now);

        InventoryAdjuster adjuster = adjusters.of(locationType);

        if (increase) {
            adjuster.increment(adjustment);
        } else {
            adjuster.decrement(adjustment);
        }
    }

    // Total stock of this product/packaging/batch at the location; a batch with
    // no inventory row yet counts as zero.
    private long currentStock(
            LocationType locationType,
            String locationId,
            ProductDetails product,
            PackagingDetails packaging,
            BatchDetails batch) {

        Long totalStock;

        if (locationType == LocationType.WAREHOUSE) {
            totalStock = warehouseInventoryRepository
                    .findByWarehouse_WarehouseIdAndProductAndPackagingAndBatch(
                            locationId, product, packaging, batch)
                    .map(WarehouseInventory::getTotalStock)
                    .orElse(0L);
        } else {
            totalStock = inventoryRepository
                    .findByPharmacy_PharmacyIdAndProductAndPackagingAndBatch(
                            locationId, product, packaging, batch)
                    .map(Inventory::getTotalStock)
                    .orElse(0L);
        }

        return totalStock != null ? totalStock : 0L;
    }

    private String generateStockAdjustmentNo(String pharmacyId) {

        int year = LocalDate.now().getYear();
        String prefix = "SA-" + year + "-";

        List<String> latest = stockAdjustmentRepository.findLatestStockAdjustmentNo(
                prefix,
                pharmacyId,
                PageRequest.of(0, 1)
        );

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestStockAdjustmentNo = latest.get(0);

            String numberPart = latestStockAdjustmentNo.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }

    private String generateStockAdjustmentNoForWarehouse(String warehouseId) {

        int year = LocalDate.now().getYear();
        String prefix = "SA-" + year + "-";

        List<String> latest = stockAdjustmentRepository.findLatestStockAdjustmentNoByWarehouse(
                prefix,
                warehouseId,
                PageRequest.of(0, 1)
        );

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestStockAdjustmentNo = latest.get(0);

            String numberPart = latestStockAdjustmentNo.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }

    private void stampAuditFields(StockAdjustment stockAdjustment, String actor, LocalDateTime now) {

        stockAdjustment.setStockAdjustmentDate(now);
        stockAdjustment.setCreatedBy(actor);
        stockAdjustment.setCreatedAt(now);
        stockAdjustment.setModifiedBy(null);
        stockAdjustment.setModifiedAt(null);
    }
}
