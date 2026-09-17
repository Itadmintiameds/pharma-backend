package tiameds.pharmabackend.service.product.bulk;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.product.bulk.BulkUploadResponse;
import tiameds.pharmabackend.dto.product.bulk.BulkUploadRowResult;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.LocationType;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.security.CustomUserDetails;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drives a bulk upload: parse, map, group by product, then hand each product to
 * {@link ProductBulkGroupWriter} for its own transaction.
 * <p>
 * Deliberately not transactional. A file of a few thousand rows will contain a
 * handful of bad ones, and failing the whole import over them forces the operator
 * into a guess-and-retry loop. Instead every line reports its own outcome and the
 * good ones land, so a second upload of the corrected file only adds what is missing
 * (batches already imported are skipped, stock included).
 */
@Service
@RequiredArgsConstructor
public class ProductBulkUploadServiceImpl implements ProductBulkUploadService {

    private final LocationContextResolver locationContextResolver;
    private final PharmacyDetailsRepository pharmacyRepo;
    private final WarehouseRepository warehouseRepo;
    private final MasterIndexLoader masterIndexLoader;
    private final ProductRowMapper rowMapper;
    private final ProductBulkGroupWriter groupWriter;

    @Override
    public BulkUploadResponse upload(MultipartFile file, boolean dryRun,
                                     String pharmacyId, String warehouseId) {
        BulkUploadResponse response = new BulkUploadResponse();
        response.setDryRun(dryRun);

        BulkUploadTarget target = resolveTarget(pharmacyId, warehouseId);
        response.setLocationType(target.type().name());
        response.setLocationId(target.locationId());
        response.setLocationName(target.locationName());

        if (file == null || file.isEmpty()) {
            response.getFileErrors().add("No file was uploaded");
            return response;
        }

        CsvFile csv;
        try {
            csv = CsvFile.parse(file.getInputStream());
        } catch (IOException e) {
            response.getFileErrors().add("Could not read the file: " + e.getMessage());
            return response;
        }

        List<String> missing = ProductBulkColumns.REQUIRED_HEADERS.stream()
                .filter(header -> !csv.hasHeader(header))
                .toList();
        if (!missing.isEmpty()) {
            response.getFileErrors().add("Missing required column(s): " + String.join(", ", missing));
            return response;
        }
        if (csv.getRows().isEmpty()) {
            response.getFileErrors().add("The file has a header but no data rows");
            return response;
        }

        MasterIndex masters = masterIndexLoader.load();

        // Map first, so an unreadable line is reported against itself and never
        // takes down the product group it happens to sit in.
        List<BulkUploadRowResult> results = new ArrayList<>();
        Map<String, List<MappedRow>> groups = new LinkedHashMap<>();

        for (CsvFile.Row row : csv.getRows()) {
            try {
                MappedRow mapped = rowMapper.map(row, masters);
                groups.computeIfAbsent(groupKey(mapped), k -> new ArrayList<>()).add(mapped);
            } catch (ProductRowMapper.RowRejectedException | CellCoercion.BulkValueException e) {
                results.add(failed(row.getLineNumber(), row, e.getMessage()));
            } catch (RuntimeException e) {
                results.add(failed(row.getLineNumber(), row,
                        "Could not read this row: " + messageOf(e)));
            }
        }

        String actor = currentActor();
        for (List<MappedRow> group : groups.values()) {
            if (dryRun) {
                group.forEach(row -> results.add(validated(row)));
                continue;
            }
            try {
                results.addAll(groupWriter.write(group, target, actor));
            } catch (RuntimeException e) {
                // The group's transaction rolled back, so every one of its lines failed.
                String reason = messageOf(e);
                for (MappedRow row : group) {
                    BulkUploadRowResult result = new BulkUploadRowResult();
                    result.setLine(row.getLine());
                    result.setProductCode(row.getProductCode());
                    result.setProductName(row.getProductName());
                    result.setBatchNumber(row.getBatch().getBatchNumber());
                    result.setStatus(BulkUploadRowResult.Status.FAILED);
                    result.setError("Product \"" + row.getProductName() + "\" could not be saved: " + reason);
                    result.getWarnings().addAll(row.getWarnings());
                    results.add(result);
                }
            }
        }

        results.sort(Comparator.comparingInt(BulkUploadRowResult::getLine));
        response.setRows(results);
        tally(response);
        return response;
    }

    @Override
    public String templateCsv() {
        return String.join(",", ProductBulkColumns.TEMPLATE) + "\n";
    }

    // ===== helpers =====

    /**
     * Lines belong to the same product when they share a product code. The code is the
     * operator's own identifier and is not stored, so when it is blank the product's
     * natural key — the same one the catalog de-duplicates on — is used instead.
     */
    private String groupKey(MappedRow row) {
        if (row.getProductCode() != null) {
            return "code:" + row.getProductCode().trim().toLowerCase();
        }
        return "name:" + lower(row.getProductName()) + "|" + lower(row.getBrandName()) + "|" + lower(row.getHsnNo());
    }

    private String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private BulkUploadRowResult failed(int line, CsvFile.Row row, String error) {
        BulkUploadRowResult result = new BulkUploadRowResult();
        result.setLine(line);
        result.setProductCode(row.get(ProductBulkColumns.PRODUCT_CODE));
        result.setProductName(row.get(ProductBulkColumns.PRODUCT_NAME));
        result.setBatchNumber(row.get(ProductBulkColumns.BATCH_NUMBER));
        result.setStatus(BulkUploadRowResult.Status.FAILED);
        result.setError(error);
        return result;
    }

    /**
     * A dry-run line: it mapped cleanly, but nothing was written.
     */
    private BulkUploadRowResult validated(MappedRow row) {
        BulkUploadRowResult result = new BulkUploadRowResult();
        result.setLine(row.getLine());
        result.setProductCode(row.getProductCode());
        result.setProductName(row.getProductName());
        result.setBatchNumber(row.getBatch().getBatchNumber());
        result.setStatus(BulkUploadRowResult.Status.CREATED);
        result.setStockAdded(row.getStockInSmallestUnits());
        result.getWarnings().addAll(row.getWarnings());
        return result;
    }

    private void tally(BulkUploadResponse response) {
        response.setTotalRows(response.getRows().size());
        // A product is counted once, on the first of its lines that created it.
        List<String> createdProducts = new ArrayList<>();
        List<String> updatedProducts = new ArrayList<>();

        for (BulkUploadRowResult row : response.getRows()) {
            switch (row.getStatus()) {
                case CREATED -> {
                    response.setBatchesCreated(response.getBatchesCreated() + 1);
                    if (row.getProductId() != null && !createdProducts.contains(row.getProductId())) {
                        createdProducts.add(row.getProductId());
                    }
                }
                case UPDATED -> {
                    response.setBatchesCreated(response.getBatchesCreated() + 1);
                    if (row.getProductId() != null && !updatedProducts.contains(row.getProductId())) {
                        updatedProducts.add(row.getProductId());
                    }
                }
                case SKIPPED -> response.setRowsSkipped(response.getRowsSkipped() + 1);
                case FAILED -> response.setRowsFailed(response.getRowsFailed() + 1);
            }
            if (row.getStockAdded() != null) {
                response.setStockAdded(response.getStockAdded() + row.getStockAdded());
            }
        }
        response.setProductsCreated(createdProducts.size());
        response.setProductsUpdated(updatedProducts.size());
    }

    /**
     * Picks the destination. An explicitly named warehouse or pharmacy wins, so an
     * operator (or an internal-API-key caller, which has no user and therefore no
     * header context) can say where the file lands. With neither named, this falls
     * back to the location the caller is already working in, exactly as onboarding does.
     * <p>
     * A named location is still checked against the logged-in user when there is one,
     * so passing an id cannot be used to write into someone else's tenant. An
     * API-key call is trusted by definition and is not narrowed further.
     */
    private BulkUploadTarget resolveTarget(String pharmacyId, String warehouseId) {
        UserDetails user = currentUserOrNull();

        if (isSet(warehouseId)) {
            Warehouse warehouse = warehouseRepo.findById(warehouseId.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Warehouse not found: " + warehouseId));
            if (user != null
                    && !locationContextResolver.warehouseInUserOrganization(warehouse.getWarehouseId(), user)) {
                throw new AccessDeniedException(
                        "You are not allowed to upload into warehouse " + warehouse.getWarehouseId());
            }
            return new BulkUploadTarget(LocationType.WAREHOUSE,
                    warehouse.getWarehouseId(), warehouse.getWarehouseName());
        }

        if (isSet(pharmacyId)) {
            PharmacyDetails pharmacy = pharmacyRepo.findById(pharmacyId.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Pharmacy not found: " + pharmacyId));
            if (user != null
                    && !pharmacyRepo.existsUserPharmacy(pharmacy.getPharmacyId(), user.getUserId())) {
                throw new AccessDeniedException(
                        "You are not allowed to upload into pharmacy " + pharmacy.getPharmacyId());
            }
            return new BulkUploadTarget(LocationType.PHARMACY,
                    pharmacy.getPharmacyId(), pharmacy.getPharmacyName());
        }

        if (user == null) {
            throw new IllegalArgumentException(
                    "Pass pharmacyId or warehouseId to say where the upload should land");
        }

        LocationContext loc = locationContextResolver.resolve(user);
        if (loc.isWarehouse()) {
            Warehouse warehouse = warehouseRepo.findById(loc.getLocationId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Warehouse not found: " + loc.getLocationId()));
            return new BulkUploadTarget(loc.getType(), warehouse.getWarehouseId(),
                    warehouse.getWarehouseName());
        }
        PharmacyDetails pharmacy = pharmacyRepo.findById(loc.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pharmacy not found: " + loc.getLocationId()));
        return new BulkUploadTarget(loc.getType(), pharmacy.getPharmacyId(), pharmacy.getPharmacyName());
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * The logged-in user, or null when the request was authenticated some other way
     * (the internal API key) and so has no user behind it.
     */
    private UserDetails currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getUser();
        }
        return null;
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "System";
        }
        if (auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getUserId();
        }
        // API-key callers authenticate as a service principal; record that as the author.
        return auth.getName() == null ? "System" : auth.getName();
    }

    private String messageOf(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
