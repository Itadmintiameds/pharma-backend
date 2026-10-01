package tiameds.pharmabackend.service.impl.warehouse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.warehouse.WarehouseReturnDetailsDto;
import tiameds.pharmabackend.dto.warehouse.WarehouseReturnDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.entity.warehouse.WarehouseReturn;
import tiameds.pharmabackend.entity.warehouse.WarehouseReturnDetails;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.enums.StockReturnStatus;
import tiameds.pharmabackend.enums.TransactionType;
import tiameds.pharmabackend.mapper.warehouse.WarehouseReturnMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.product.BatchDetailsRepository;
import tiameds.pharmabackend.repository.product.ProductDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseReturnRepository;
import tiameds.pharmabackend.service.impl.warehouse.stock.InventoryAdjusters;
import tiameds.pharmabackend.service.warehouse.WarehouseReturnService;
import tiameds.pharmabackend.service.warehouse.stock.InventoryAdjuster;
import tiameds.pharmabackend.service.warehouse.stock.StockAdjustment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseReturnServiceImpl implements WarehouseReturnService {

    private final WarehouseReturnRepository warehouseReturnRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final ProductDetailsRepository pharmaProductDetailsRepository;
    private final BatchDetailsRepository pharmaBatchDetailsRepository;
    private final LocationContextResolver locationContextResolver;
    private final InventoryAdjusters adjusters;

    @Override
    public WarehouseReturnDto createWarehouseReturn(WarehouseReturnDto warehouseReturnDto, UserDetails user) {

        if (warehouseReturnDto == null) {
            throw new RuntimeException("Warehouse return details are required");
        }

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = resolveUserPharmacy(persistentUser);

        String warehouseId = warehouseReturnDto.getToWarehouseId();

        if (warehouseId == null || warehouseId.isBlank()) {
            throw new RuntimeException("Warehouse id is required");
        }

        if (!locationContextResolver.warehouseInUserOrganization(warehouseId, persistentUser)) {
            throw new RuntimeException("This warehouse does not belong to your organization.");
        }

        if (warehouseReturnDto.getWarehouseReturnDetails() == null
                || warehouseReturnDto.getWarehouseReturnDetails().isEmpty()) {
            throw new RuntimeException("At least one warehouse return line is required");
        }

        WarehouseReturn warehouseReturn = WarehouseReturnMapper.toEntity(warehouseReturnDto);

        warehouseReturn.setWarehouseReturnId(null);
        warehouseReturn.setFromPharmacyId(pharmacyId);
        warehouseReturn.setToWarehouseId(warehouseId);
        warehouseReturn.setStockReturnNo(generateStockReturnNo(pharmacyId));
        warehouseReturn.setStockReturnStatus(resolveCreateStatus(warehouseReturnDto.getStockReturnStatus()));
        warehouseReturn.setIsDelete(false);

        resolveReturnDetails(warehouseReturn, warehouseReturnDto);

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        stampAuditFields(warehouseReturn, actor, now);

        WarehouseReturn savedWarehouseReturn = warehouseReturnRepository.save(warehouseReturn);

        return WarehouseReturnMapper.toDto(savedWarehouseReturn);
    }

    @Override
    public List<WarehouseReturnDto> getAllWarehouseReturns(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isWarehouse()) {
            return warehouseReturnRepository.findByToWarehouseId(location.getLocationId())
                    .stream()
                    .map(WarehouseReturnMapper::toDto)
                    .collect(Collectors.toList());
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return warehouseReturnRepository.findByFromPharmacyId(pharmacyId)
                .stream()
                .map(WarehouseReturnMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public WarehouseReturnDto getWarehouseReturnById(Long warehouseReturnId, UserDetails user) {

        if (warehouseReturnId == null) {
            throw new RuntimeException("Warehouse return id is required");
        }

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        WarehouseReturn warehouseReturn = warehouseReturnRepository.findActiveByIdWithDetails(warehouseReturnId)
                .orElseThrow(() -> new RuntimeException("Warehouse return not found: " + warehouseReturnId));

        if (location.isWarehouse()) {

            if (!location.getLocationId().equals(warehouseReturn.getToWarehouseId())) {
                throw new RuntimeException("This warehouse return was not sent to your warehouse.");
            }

            return WarehouseReturnMapper.toDto(warehouseReturn);
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        if (!pharmacyId.equals(warehouseReturn.getFromPharmacyId())) {
            throw new RuntimeException("This warehouse return does not belong to your pharmacy.");
        }

        return WarehouseReturnMapper.toDto(warehouseReturn);
    }

    @Override
    public WarehouseReturnDto dispatchWarehouseReturn(Long warehouseReturnId, UserDetails user) {

        if (warehouseReturnId == null) {
            throw new RuntimeException("Warehouse return id is required");
        }

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = resolveUserPharmacy(persistentUser);

        WarehouseReturn warehouseReturn = warehouseReturnRepository.findById(warehouseReturnId)
                .filter(wr -> !Boolean.TRUE.equals(wr.getIsDelete()))
                .orElseThrow(() -> new RuntimeException("Warehouse return not found: " + warehouseReturnId));

        if (!pharmacyId.equals(warehouseReturn.getFromPharmacyId())) {
            throw new RuntimeException("This warehouse return does not belong to your pharmacy.");
        }

        // Only a draft may be dispatched, so the pharmacy stock can never be
        // taken out twice for the same return.
        if (warehouseReturn.getStockReturnStatus() != StockReturnStatus.DRAFT) {
            throw new RuntimeException(
                    "Only a Draft warehouse return can be dispatched. Current status: "
                            + warehouseReturn.getStockReturnStatus().getLabel());
        }

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        InventoryAdjuster pharmacyAdjuster = adjusters.of(LocationType.PHARMACY);

        for (WarehouseReturnDetails detail : warehouseReturn.getWarehouseReturnDetails()) {

            // Every line is sent in full: what goes out is what was put on the return.
            long dispatched = detail.getReturnQuantity() != null ? detail.getReturnQuantity() : 0L;

            detail.setDispatchQuantity(dispatched);
            detail.setModifiedBy(actor);
            detail.setModifiedAt(now);

            if (dispatched == 0) {
                continue;
            }

            BatchDetails batch = detail.getBatch();

            // OUT leg against the pharmacy: checks there is enough stock, lowers
            // pharma_inventory and writes the pharma_inventory_audit row.
            pharmacyAdjuster.decrement(new StockAdjustment(
                    pharmacyId,
                    detail.getProduct(),
                    batch.getPackagingDetails(),
                    batch,
                    dispatched,
                    TransactionType.STOCK_RETURN,
                    null,   // not a distribution line
                    null,   // not a purchase line
                    actor,
                    now));
        }

        warehouseReturn.setStockReturnStatus(StockReturnStatus.PENDING_RECEIPT);
        warehouseReturn.setModifiedBy(actor);
        warehouseReturn.setModifiedAt(now);

        WarehouseReturn savedWarehouseReturn = warehouseReturnRepository.save(warehouseReturn);

        return WarehouseReturnMapper.toDto(savedWarehouseReturn);
    }

    @Override
    public WarehouseReturnDto receiveWarehouseReturn(
            Long warehouseReturnId,
            WarehouseReturnDto warehouseReturnDto,
            UserDetails user) {

        if (warehouseReturnId == null) {
            throw new RuntimeException("Warehouse return id is required");
        }

        if (warehouseReturnDto == null
                || warehouseReturnDto.getWarehouseReturnDetails() == null
                || warehouseReturnDto.getWarehouseReturnDetails().isEmpty()) {
            throw new RuntimeException("Received quantities are required for every warehouse return line");
        }

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // The receiving side of a return is the warehouse it was sent to.
        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (!location.isWarehouse()) {
            throw new RuntimeException("A warehouse return can only be received by a warehouse.");
        }

        String warehouseId = location.getLocationId();

        WarehouseReturn warehouseReturn = warehouseReturnRepository.findById(warehouseReturnId)
                .filter(wr -> !Boolean.TRUE.equals(wr.getIsDelete()))
                .orElseThrow(() -> new RuntimeException("Warehouse return not found: " + warehouseReturnId));

        if (!warehouseId.equals(warehouseReturn.getToWarehouseId())) {
            throw new RuntimeException("This warehouse return was not sent to your warehouse.");
        }

        // Only dispatched goods can be received, and only once.
        if (warehouseReturn.getStockReturnStatus() != StockReturnStatus.PENDING_RECEIPT) {
            throw new RuntimeException(
                    "Only a Pending Receipt warehouse return can be received. Current status: "
                            + warehouseReturn.getStockReturnStatus().getLabel());
        }

        Map<Long, WarehouseReturnDetailsDto> receivedByLine = receivedLinesById(warehouseReturnDto);

        String actor = String.valueOf(persistentUser.getUserId());
        LocalDateTime now = LocalDateTime.now();

        InventoryAdjuster warehouseAdjuster = adjusters.of(LocationType.WAREHOUSE);

        for (WarehouseReturnDetails detail : warehouseReturn.getWarehouseReturnDetails()) {

            WarehouseReturnDetailsDto line = receivedByLine.remove(detail.getWarehouseReturnDetailId());

            if (line == null) {
                throw new RuntimeException(
                        "Received quantity is missing for line " + detail.getWarehouseReturnDetailId());
            }

            long dispatched = detail.getDispatchQuantity() != null ? detail.getDispatchQuantity() : 0L;

            if (line.getReceivedQuantity() > dispatched) {
                throw new RuntimeException(
                        "Received quantity (" + line.getReceivedQuantity()
                                + ") cannot exceed the dispatched quantity (" + dispatched
                                + ") for line " + detail.getWarehouseReturnDetailId());
            }

            detail.setReceivedQuantity(line.getReceivedQuantity());
            detail.setNotReceivedQuantity(line.getNotReceivedQuantity());
            detail.setModifiedBy(actor);
            detail.setModifiedAt(now);

            if (line.getReceivedQuantity() == 0) {
                continue;
            }

            BatchDetails batch = detail.getBatch();

            // IN leg against the warehouse: only what actually arrived goes into
            // stock. Finds or creates the pharma_warehouse_inventory row, raises it
            // and writes the pharma_warehouse_inventory_audit row.
            warehouseAdjuster.increment(new StockAdjustment(
                    warehouseId,
                    detail.getProduct(),
                    batch.getPackagingDetails(),
                    batch,
                    line.getReceivedQuantity(),
                    TransactionType.STOCK_RETURN,
                    null,   // not a distribution line
                    null,   // not a purchase line
                    actor,
                    now));
        }

        // Anything left over was sent for a line this return does not have.
        if (!receivedByLine.isEmpty()) {
            throw new RuntimeException(
                    "Lines do not belong to warehouse return " + warehouseReturnId + ": "
                            + receivedByLine.keySet());
        }

        // Header totals come from the frontend, the same as on create.
        warehouseReturn.setTotalReceivedQuantity(warehouseReturnDto.getTotalReceivedQuantity());
        warehouseReturn.setTotalNotReceivedQuantity(warehouseReturnDto.getTotalNotReceivedQuantity());
        warehouseReturn.setStockReturnStatus(StockReturnStatus.COMPLETE);
        warehouseReturn.setModifiedBy(actor);
        warehouseReturn.setModifiedAt(now);

        WarehouseReturn savedWarehouseReturn = warehouseReturnRepository.save(warehouseReturn);

        return WarehouseReturnMapper.toDto(savedWarehouseReturn);
    }

    private Map<Long, WarehouseReturnDetailsDto> receivedLinesById(WarehouseReturnDto warehouseReturnDto) {

        Map<Long, WarehouseReturnDetailsDto> byLine = new HashMap<>();

        for (WarehouseReturnDetailsDto line : warehouseReturnDto.getWarehouseReturnDetails()) {

            Long detailId = line.getWarehouseReturnDetailId();

            if (detailId == null) {
                throw new RuntimeException("warehouseReturnDetailId is required on every received line");
            }

            if (line.getReceivedQuantity() == null || line.getReceivedQuantity() < 0) {
                throw new RuntimeException("Received quantity must be zero or positive on line " + detailId);
            }

            if (line.getNotReceivedQuantity() == null || line.getNotReceivedQuantity() < 0) {
                throw new RuntimeException("Not received quantity must be zero or positive on line " + detailId);
            }

            if (byLine.put(detailId, line) != null) {
                throw new RuntimeException("Warehouse return line " + detailId + " is sent more than once");
            }
        }

        return byLine;
    }

    // A stock return always goes from a pharmacy back to a warehouse, so it can
    // only be raised or dispatched from a pharmacy the user belongs to.
    private String resolveUserPharmacy(UserDetails persistentUser) {

        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (!location.isPharmacy()) {
            throw new RuntimeException("A warehouse return can only be raised from a pharmacy.");
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return pharmacyId;
    }

    // A return is always created as DRAFT. It moves to PENDING_RECEIPT only through
    // dispatch, which is where the pharmacy stock is taken out.
    private StockReturnStatus resolveCreateStatus(StockReturnStatus requested) {

        if (requested != null && requested != StockReturnStatus.DRAFT) {
            throw new RuntimeException(
                    "A warehouse return can only be created as Draft. Use dispatch to send it.");
        }

        return StockReturnStatus.DRAFT;
    }

    // Resolves the managed product/batch for each return line (same index-aligned
    // pattern PurchaseReturnServiceImpl uses).
    private void resolveReturnDetails(WarehouseReturn warehouseReturn, WarehouseReturnDto warehouseReturnDto) {

        for (int i = 0; i < warehouseReturn.getWarehouseReturnDetails().size(); i++) {

            WarehouseReturnDetails detail = warehouseReturn.getWarehouseReturnDetails().get(i);
            WarehouseReturnDetailsDto dto = warehouseReturnDto.getWarehouseReturnDetails().get(i);

            if (dto.getProductId() == null || dto.getBatchId() == null) {
                throw new RuntimeException("Product and batch are required on every return line");
            }

            if (dto.getReturnQuantity() == null || dto.getReturnQuantity() <= 0) {
                throw new RuntimeException(
                        "Return quantity must be greater than zero for product " + dto.getProductId());
            }

            ProductDetails product = pharmaProductDetailsRepository
                    .findById(dto.getProductId())
                    .orElseThrow(() ->
                            new RuntimeException("Product not found: " + dto.getProductId()));

            BatchDetails batch = pharmaBatchDetailsRepository
                    .findById(dto.getBatchId())
                    .orElseThrow(() ->
                            new RuntimeException("Batch not found: " + dto.getBatchId()));

            detail.setWarehouseReturnDetailId(null);
            detail.setProduct(product);
            detail.setBatch(batch);
        }
    }

    private String generateStockReturnNo(String pharmacyId) {

        int year = LocalDate.now().getYear();
        String prefix = "STR-" + year + "-";

        List<String> latest = warehouseReturnRepository.findLatestStockReturnNo(
                prefix,
                pharmacyId,
                PageRequest.of(0, 1)
        );

        int nextNumber = 1;

        if (!latest.isEmpty()) {

            String latestStockReturnNo = latest.get(0);

            String numberPart = latestStockReturnNo.substring(prefix.length());

            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }

    private void stampAuditFields(WarehouseReturn warehouseReturn, String actor, LocalDateTime now) {

        warehouseReturn.setStockReturnDate(now);
        warehouseReturn.setCreatedBy(actor);
        warehouseReturn.setCreatedAt(now);
        warehouseReturn.setModifiedBy(null);
        warehouseReturn.setModifiedAt(null);

        for (WarehouseReturnDetails detail : warehouseReturn.getWarehouseReturnDetails()) {

            detail.setWarehouseReturn(warehouseReturn);
            detail.setCreatedBy(actor);
            detail.setCreatedAt(now);
            detail.setModifiedBy(null);
            detail.setModifiedAt(null);
        }
    }
}
