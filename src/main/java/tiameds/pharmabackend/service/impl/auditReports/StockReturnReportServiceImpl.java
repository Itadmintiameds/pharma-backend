package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.StockReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockReturnReportPageDto;
import tiameds.pharmabackend.dto.auditReports.StockReturnReportResponse;
import tiameds.pharmabackend.dto.auditReports.StockReturnReportRowDto;
import tiameds.pharmabackend.dto.auditReports.StockReturnReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;
import tiameds.pharmabackend.enums.StockReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.auditReports.StockReturnReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;
import tiameds.pharmabackend.service.impl.financialReport.export.ExportFormat;
import tiameds.pharmabackend.service.impl.financialReport.export.ReportTableWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockReturnReportServiceImpl implements StockReturnReportService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    // Print / PDF (unpaged) is refused above this many rows; the client
    // should narrow the filters or download Excel instead.
    private static final int PRINT_ROW_LIMIT = 2000;

    // The CSV / Excel export reads the report in batches of this size, so
    // memory use stays flat however many rows there are.
    private static final int EXPORT_BATCH_SIZE = 2000;

    // An .xlsx sheet holds 1,048,576 rows; the title block needs a few.
    private static final long EXCEL_ROW_LIMIT = 1_048_000;

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    // The batch expiry, as the screen shows it: "12/2027".
    private static final DateTimeFormatter EXPIRY_MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    private static final List<String> EXPORT_HEADERS = List.of(
            "Return Date", "Reference Number", "Return Type", "Source", "Destination",
            "Product Name", "Batch No", "Expiry", "Return Reason", "Returned Quantity");

    // Return Reason column of the export, as the screen words it.
    private static final Map<String, String> RETURN_REASON_LABELS = Map.of(
            "DAMAGE", "Damage",
            "EXPIRY", "Expiry",
            "OTHER", "Other");

    // Return Type dropdown: code -> label. The codes are built in the base
    // CTE as source_type || '_TO_' || destination_type.
    private static final Map<String, String> RETURN_TYPES = new LinkedHashMap<>();

    static {
        RETURN_TYPES.put("PHARMACY_TO_SUPPLIER", "Pharmacy → Supplier");
        RETURN_TYPES.put("PHARMACY_TO_WAREHOUSE", "Pharmacy → Warehouse");
        RETURN_TYPES.put("WAREHOUSE_TO_SUPPLIER", "Warehouse → Supplier");
    }

    // Return Reason dropdown codes, produced by the reason_category CASE in
    // buildReportSql.
    private static final Set<String> RETURN_REASONS = new LinkedHashSet<>(List.of("DAMAGE", "EXPIRY", "OTHER"));

    private static final Set<String> SOURCE_LOCATION_TYPES = new LinkedHashSet<>(List.of("PHARMACY", "WAREHOUSE"));
    private static final Set<String> DESTINATION_TYPES = new LinkedHashSet<>(List.of("SUPPLIER", "WAREHOUSE"));

    // Return reasons are free text ("Damaged packaging", "Near expiry"), so
    // they are classified by wording (case-insensitive POSIX regex).
    private static final String DAMAGE_REASON_PATTERN = "damag|broke|breakage";
    private static final String EXPIRY_REASON_PATTERN = "expir";

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("returnDate", "return_date"),
            Map.entry("referenceNumber", "reference_number"),
            Map.entry("returnType", "return_type"),
            Map.entry("source", "source_name"),
            Map.entry("destination", "destination_name"),
            Map.entry("productName", "product_name"),
            Map.entry("batchNo", "batch_number"),
            Map.entry("expiryDate", "expiry_date"),
            Map.entry("returnReason", "reason_category"),
            Map.entry("returnedQuantity", "returned_quantity"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public StockReturnReportResponse getStockReturnReport(
            StockReturnReportFilter filter,
            UserDetails user) {

        ReportQuery report = prepareReport(filter, user);

        StockReturnReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        long totalRecords = fetchTotalRecords(report.sql(), report.params());

        int size;
        int page;

        if (Boolean.TRUE.equals(filter.getUnpaged())) {
            if (totalRecords > PRINT_ROW_LIMIT) {
                throw new IllegalArgumentException(
                        "Too many rows to print (" + totalRecords + "). The limit is " + PRINT_ROW_LIMIT
                                + ". Narrow the filters or download Excel.");
            }
            size = (int) Math.max(totalRecords, 1);
            page = 0;
        } else {
            size = filter.getSize() == null || filter.getSize() < 1
                    ? DEFAULT_PAGE_SIZE
                    : Math.min(filter.getSize(), MAX_PAGE_SIZE);
            page = filter.getPage() == null || filter.getPage() < 0 ? 0 : filter.getPage();
        }

        long offset = (long) page * size;

        StockReturnReportPageDto details = new StockReturnReportPageDto();
        details.setData(fetchPage(report.sql(), report.params(), filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        StockReturnReportResponse response = new StockReturnReportResponse();
        response.setSummary(summary);
        response.setReturnTransactions(details);
        response.setGeneratedBy(report.user().getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Runs the count and the KPI totals now, so bad filters, missing access
    // and the Excel row limit fail before any of the file has been sent.
    @Override
    public ReportExport exportStockReturnReport(
            StockReturnReportFilter filter,
            String format,
            UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        ReportQuery report = prepareReport(filter, user);

        long totalRecords = fetchTotalRecords(report.sql(), report.params());

        if (exportFormat == ExportFormat.XLSX && totalRecords > EXCEL_ROW_LIMIT) {
            throw new IllegalArgumentException(
                    "Too many rows for Excel (" + totalRecords + "). Narrow the filters or download CSV.");
        }

        StockReturnReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        String generatedBy = report.user().getFullName();
        LocalDateTime generatedOn = LocalDateTime.now();
        String pharmacies = pharmacyNames(report.pharmacyIds());
        String warehouses = warehouseNames(report.warehouseIds());

        boolean singleDay = report.fromDate().equals(report.toDate());
        String period = singleDay
                ? report.fromDate().format(DISPLAY_DATE)
                : report.fromDate().format(DISPLAY_DATE) + " to " + report.toDate().format(DISPLAY_DATE);
        // stock-return_2026-10-08, stock-return_2026-10-01_to_2026-10-08
        String fileName = "stock-return_"
                + (singleDay ? report.fromDate() : report.fromDate() + "_to_" + report.toDate())
                + exportFormat.extension();

        return new ReportExport() {

            @Override
            public String fileName() {
                return fileName;
            }

            @Override
            public String contentType() {
                return exportFormat.contentType();
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {

                try (ReportTableWriter writer = exportFormat.open(out)) {

                    writer.title("Stock Return Report");
                    if (!pharmacies.isEmpty()) {
                        writer.line("Pharmacy", pharmacies);
                    }
                    if (!warehouses.isEmpty()) {
                        writer.line("Warehouse", warehouses);
                    }
                    writer.line("Period", period);
                    writer.line("Generated By", generatedBy);
                    writer.line("Generated On", generatedOn);
                    writer.blank();

                    writer.line("Total Returned Quantity", summary.getTotalReturnedQuantity());
                    writer.line("Damage Quantity", summary.getDamageQuantity());
                    writer.line("Expiry Quantity", summary.getExpiryQuantity());
                    writer.blank();

                    writer.header(EXPORT_HEADERS);

                    for (long offset = 0; offset < totalRecords; offset += EXPORT_BATCH_SIZE) {

                        List<StockReturnReportRowDto> batch =
                                fetchPage(report.sql(), report.params(), filter, EXPORT_BATCH_SIZE, offset);

                        for (StockReturnReportRowDto row : batch) {
                            writer.row(Arrays.asList(
                                    row.getReturnDate(), row.getReferenceNumber(), row.getReturnTypeLabel(),
                                    row.getSourceName(), row.getDestinationName(),
                                    row.getProductName(), row.getBatchNo(),
                                    row.getExpiryDate() == null ? null : row.getExpiryDate().format(EXPIRY_MONTH),
                                    RETURN_REASON_LABELS.getOrDefault(row.getReturnReason(), row.getReturnReason()),
                                    row.getReturnedQuantity()));
                        }

                        if (batch.isEmpty()) {
                            break;
                        }
                    }

                    writer.finish();
                }
            }
        };
    }


    // The report as the filters define it, shared by the screen, print and
    // export: the user, the resolved period and locations, and the CTE SQL
    // with its parameters.
    private record ReportQuery(
            UserDetails user,
            LocalDate fromDate,
            LocalDate toDate,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            String sql,
            Map<String, Object> params) {
    }


    private ReportQuery prepareReport(StockReturnReportFilter filter, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate fromDate = filter.getFromDate() != null ? filter.getFromDate() : LocalDate.now();
        LocalDate toDate = filter.getToDate() != null ? filter.getToDate() : fromDate;

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate cannot be before fromDate.");
        }

        List<String> pharmacyIds;
        List<String> warehouseIds;

        boolean noLocationSent = isEmpty(filter.getPharmacyIds()) && isEmpty(filter.getWarehouseIds());

        if (noLocationSent) {
            pharmacyIds = accessiblePharmacyIds(persistentUser);
            warehouseIds = accessibleWarehouseIds(persistentUser);
        } else {
            pharmacyIds = isEmpty(filter.getPharmacyIds())
                    ? List.of()
                    : checkAccess(filter.getPharmacyIds(), accessiblePharmacyIds(persistentUser), "pharmacy");
            warehouseIds = isEmpty(filter.getWarehouseIds())
                    ? List.of()
                    : checkAccess(filter.getWarehouseIds(), accessibleWarehouseIds(persistentUser), "warehouse");
        }

        if (pharmacyIds.isEmpty() && warehouseIds.isEmpty()) {
            throw new AccessDeniedException("You do not have access to any pharmacy or warehouse.");
        }

        Map<String, Object> params = new HashMap<>();
        String reportSql = buildReportSql(filter, pharmacyIds, warehouseIds, fromDate, toDate, params);

        return new ReportQuery(persistentUser, fromDate, toDate, pharmacyIds, warehouseIds, reportSql, params);
    }


    // Names in the order the ids were given; an unknown id is shown as is.
    private String pharmacyNames(List<String> pharmacyIds) {

        Map<String, String> names = new HashMap<>();

        for (PharmacyDetails pharmacy : pharmacyDetailsRepository.findAllById(pharmacyIds)) {
            names.put(pharmacy.getPharmacyId(), pharmacy.getPharmacyName());
        }

        return String.join(", ", pharmacyIds.stream()
                .map(id -> hasText(names.get(id)) ? names.get(id) : id)
                .toList());
    }


    private String warehouseNames(List<String> warehouseIds) {

        Map<String, String> names = new HashMap<>();

        for (Warehouse warehouse : warehouseRepository.findAllById(warehouseIds)) {
            names.put(warehouse.getWarehouseId(), warehouse.getWarehouseName());
        }

        return String.join(", ", warehouseIds.stream()
                .map(id -> hasText(names.get(id)) ? names.get(id) : id)
                .toList());
    }


    // Every pharmacy in the organization for a SUPER ADMIN, otherwise the
    // ones mapped to the user.
    private List<String> accessiblePharmacyIds(UserDetails user) {

        if (locationContextResolver.isSuperAdmin(user) && user.getOrganization() != null) {
            return pharmacyDetailsRepository
                    .findAllByOrganization_OrganizationIdOrderByPharmacyCity(
                            user.getOrganization().getOrganizationId())
                    .stream()
                    .map(PharmacyDetails::getPharmacyId)
                    .toList();
        }
        return pharmacyDetailsRepository.findPharmacyIdsByUserId(user.getUserId());
    }


    // Every warehouse in the organization for a SUPER ADMIN, otherwise the
    // ones mapped to the user (empty where warehouses are not in use).
    private List<String> accessibleWarehouseIds(UserDetails user) {

        if (locationContextResolver.isSuperAdmin(user) && user.getOrganization() != null) {
            return warehouseRepository
                    .findAllByOrganization_OrganizationIdOrderByWarehouseName(
                            user.getOrganization().getOrganizationId())
                    .stream()
                    .map(Warehouse::getWarehouseId)
                    .toList();
        }
        return userDetailsRepository.findWarehouseIdsByUserId(user.getUserId());
    }


    private List<String> checkAccess(List<String> requested, List<String> accessible, String kind) {

        List<String> denied = requested.stream()
                .filter(id -> !accessible.contains(id))
                .toList();

        if (!denied.isEmpty()) {
            throw new AccessDeniedException(
                    "You are not authorized to view " + kind + ": " + String.join(", ", denied));
        }

        return requested.stream().distinct().toList();
    }


    /*
     * Builds the report as a CTE chain; callers wrap it with SELECT ... FROM report.
     *
     *   txn    - one row per return line in the period:
     *              warehouse returns (pharmacy -> warehouse) that have been sent
     *              (not Draft, not deleted), in scope through the source pharmacy
     *              or the destination warehouse;
     *              confirmed purchase returns (pharmacy / warehouse -> supplier),
     *              in scope through their own location.
     *            Each line gets a reason category: the line's own reason wins;
     *            without a recognisable one a warehouse return falls back to
     *            its header (raised from a damaged distribution line, or a
     *            return type such as "EXPIRED").
     *   base   - txn joined to product, batch, location / supplier names, with
     *            the supplier returned to or, for a return to a warehouse, the
     *            supplier of the first purchase invoice the batch came in on;
     *            narrowed by the product filters
     *   report - base narrowed by supplier, return type, reason, reference
     *            number, source location type and destination type
     */
    private String buildReportSql(
            StockReturnReportFilter filter,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        params.put("fromTs", Timestamp.valueOf(fromDate.atStartOfDay()));
        params.put("toTs", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        params.put("draftReturnStatus", StockReturnStatus.DRAFT.getLabel());
        params.put("confirmedPurchaseReturn", PurchaseReturnStatus.CONFIRMED.name());
        params.put("damagePattern", DAMAGE_REASON_PATTERN);
        params.put("expiryPattern", EXPIRY_REASON_PATTERN);

        // An empty list in IN (...) is invalid SQL, so each location kind is
        // only included when it has ids.
        List<String> stockReturnScope = new ArrayList<>();
        List<String> ownScope = new ArrayList<>();

        if (!pharmacyIds.isEmpty()) {
            params.put("pharmacyIds", pharmacyIds);
            stockReturnScope.add("x.from_pharmacy_id IN (:pharmacyIds)");
            ownScope.add("x.pharmacy_id IN (:pharmacyIds)");
        }

        if (!warehouseIds.isEmpty()) {
            params.put("warehouseIds", warehouseIds);
            stockReturnScope.add("x.to_warehouse_id IN (:warehouseIds)");
            ownScope.add("x.warehouse_id IN (:warehouseIds)");
        }

        // Each fragment starts with a space so it never glues onto the
        // previous one (a text block would drop that leading space).
        StringBuilder productFilters = new StringBuilder();

        if (!isEmpty(filter.getProductNames())) {
            productFilters.append(" AND LOWER(TRIM(p.product_name)) IN (:productNames)");
            params.put("productNames", lowerTrimmed(filter.getProductNames()));
        }

        if (!isEmpty(filter.getProductCategoryIds())) {
            productFilters.append(" AND p.product_category_id IN (:productCategoryIds)");
            params.put("productCategoryIds", filter.getProductCategoryIds());
        }

        if (!isEmpty(filter.getBrandNames())) {
            productFilters.append(" AND LOWER(TRIM(p.brand_name)) IN (:brandNames)");
            params.put("brandNames", lowerTrimmed(filter.getBrandNames()));
        }

        if (hasText(filter.getBatchNo())) {
            productFilters.append(" AND b.batch_number ILIKE :batchNo");
            params.put("batchNo", "%" + escapeLike(filter.getBatchNo().trim()) + "%");
        }

        StringBuilder reportFilters = new StringBuilder();

        if (!isEmpty(filter.getSupplierIds())) {
            reportFilters.append(" AND supplier_id IN (:supplierIds)");
            params.put("supplierIds", filter.getSupplierIds());
        }

        List<String> returnTypes = parseCodes(filter.getReturnTypes(), RETURN_TYPES.keySet(), "returnType");
        if (!returnTypes.isEmpty()) {
            reportFilters.append(" AND return_type IN (:returnTypes)");
            params.put("returnTypes", returnTypes);
        }

        appendReasonFilter(filter.getReturnReasons(), reportFilters, params);

        if (hasText(filter.getReferenceNumber())) {
            reportFilters.append(" AND reference_number ILIKE :referenceNumber");
            params.put("referenceNumber", "%" + escapeLike(filter.getReferenceNumber().trim()) + "%");
        }

        List<String> sourceTypes = parseCodes(
                filter.getSourceLocationTypes(), SOURCE_LOCATION_TYPES, "sourceLocationType");
        if (!sourceTypes.isEmpty()) {
            reportFilters.append(" AND source_type IN (:sourceTypes)");
            params.put("sourceTypes", sourceTypes);
        }

        List<String> destinationTypes = parseCodes(
                filter.getDestinationTypes(), DESTINATION_TYPES, "destinationType");
        if (!destinationTypes.isEmpty()) {
            reportFilters.append(" AND destination_type IN (:destinationTypes)");
            params.put("destinationTypes", destinationTypes);
        }

        return """
                WITH txn AS (
                    SELECT 1 AS src_order,
                           d.warehouse_return_detail_id AS row_id,
                           x.stock_return_date AS return_date,
                           x.stock_return_no AS reference_number,
                           d.product_id, d.batch_id,
                           CAST('PHARMACY' AS VARCHAR) AS source_type,
                           CAST(x.from_pharmacy_id AS VARCHAR) AS source_id,
                           CAST('WAREHOUSE' AS VARCHAR) AS destination_type,
                           CAST(x.to_warehouse_id AS VARCHAR) AS destination_id,
                           CAST(NULL AS BIGINT) AS return_supplier_id,
                           CASE WHEN d.return_reason ~* :damagePattern THEN 'DAMAGE'
                                WHEN d.return_reason ~* :expiryPattern THEN 'EXPIRY'
                                WHEN d.warehouse_distribution_details_id IS NOT NULL
                                  OR x.stock_return_type ~* :damagePattern THEN 'DAMAGE'
                                WHEN x.stock_return_type ~* :expiryPattern THEN 'EXPIRY'
                                ELSE 'OTHER'
                           END AS reason_category,
                           d.return_reason AS return_reason_remarks,
                           COALESCE(d.dispatch_quantity, d.return_quantity, 0) AS returned_quantity
                    FROM pharma_warehouse_return x
                    JOIN pharma_warehouse_return_details d
                      ON d.warehouse_return_id = x.warehouse_return_id
                    WHERE COALESCE(x.is_delete, FALSE) = FALSE
                      AND x.stock_return_status <> :draftReturnStatus
                      AND x.stock_return_date >= :fromTs
                      AND x.stock_return_date <  :toTs
                      AND (%1$s)

                    UNION ALL

                    SELECT 2,
                           d.purchase_return_detail_id,
                           x.purchase_return_date,
                           x.return_no,
                           d.product_id, d.batch_id,
                           CASE WHEN x.warehouse_id IS NOT NULL THEN 'WAREHOUSE' ELSE 'PHARMACY' END,
                           COALESCE(x.warehouse_id, x.pharmacy_id),
                           'SUPPLIER',
                           CAST(pu.supplier_id AS VARCHAR),
                           pu.supplier_id,
                           CASE WHEN d.return_reason ~* :damagePattern THEN 'DAMAGE'
                                WHEN d.return_reason ~* :expiryPattern THEN 'EXPIRY'
                                ELSE 'OTHER'
                           END,
                           d.return_reason,
                           COALESCE(d.purchase_return_quantity, 0) + COALESCE(d.free_return_quantity, 0)
                    FROM pharma_purchase_return x
                    JOIN pharma_purchase_return_details d
                      ON d.purchase_return_id = x.purchase_return_id
                    LEFT JOIN pharma_purchase pu ON pu.purchase_id = x.purchase_id
                    WHERE x.status = :confirmedPurchaseReturn
                      AND COALESCE(x.is_draft_deleted, FALSE) = FALSE
                      AND COALESCE(d.is_active, TRUE) = TRUE
                      AND x.purchase_return_date >= :fromTs
                      AND x.purchase_return_date <  :toTs
                      AND (%2$s)
                ),
                base AS (
                    SELECT t.*,
                           t.source_type || '_TO_' || t.destination_type AS return_type,
                           CASE WHEN t.source_type = 'WAREHOUSE' THEN swh.warehouse_name
                                ELSE sph.pharmacy_name END AS source_name,
                           CASE WHEN t.destination_type = 'WAREHOUSE' THEN dwh.warehouse_name
                                ELSE dsup.supplier_name END AS destination_name,
                           p.product_name,
                           b.batch_number,
                           b.expiry_date,
                           COALESCE(t.return_supplier_id, sup.supplier_id) AS supplier_id
                    FROM txn t
                    JOIN pharma_product_details p ON p.product_id = t.product_id
                    LEFT JOIN pharma_batch_details b ON b.batch_id = t.batch_id
                    LEFT JOIN pharma_pharmacy_details sph
                           ON t.source_type = 'PHARMACY' AND sph.pharmacy_id = t.source_id
                    LEFT JOIN pharma_warehouse swh
                           ON t.source_type = 'WAREHOUSE' AND swh.warehouse_id = t.source_id
                    LEFT JOIN pharma_warehouse dwh
                           ON t.destination_type = 'WAREHOUSE' AND dwh.warehouse_id = t.destination_id
                    LEFT JOIN pharma_supplier_master dsup
                           ON t.destination_type = 'SUPPLIER' AND dsup.supplier_id = t.return_supplier_id
                    LEFT JOIN LATERAL (
                        SELECT pu.supplier_id
                        FROM pharma_purchase_details pd
                        JOIN pharma_purchase pu ON pu.purchase_id = pd.purchase_id
                        WHERE t.return_supplier_id IS NULL
                          AND pd.batch_id = t.batch_id
                        ORDER BY pd.purchase_details_id
                        LIMIT 1
                    ) sup ON TRUE
                    WHERE 1 = 1
                      %3$s
                ),
                report AS (
                    SELECT * FROM base WHERE 1 = 1 %4$s
                )
                """.formatted(
                String.join(" OR ", stockReturnScope),
                String.join(" OR ", ownScope),
                productFilters,
                reportFilters);
    }


    // DAMAGE / EXPIRY / OTHER (or their labels) filter on the category; any
    // other value is a configured reason matched against the entered text.
    // ALL means no restriction.
    private void appendReasonFilter(
            List<String> requested,
            StringBuilder reportFilters,
            Map<String, Object> params) {

        if (isEmpty(requested)) {
            return;
        }

        Set<String> categories = new LinkedHashSet<>();
        Set<String> reasonTexts = new LinkedHashSet<>();

        for (String value : requested) {
            if (!hasText(value)) {
                continue;
            }

            String code = normalizeCode(value);

            if ("ALL".equals(code)) {
                return;
            }

            if (RETURN_REASONS.contains(code)) {
                categories.add(code);
            } else {
                reasonTexts.add(value.trim().toLowerCase());
            }
        }

        List<String> parts = new ArrayList<>();

        if (!categories.isEmpty()) {
            parts.add("reason_category IN (:reasonCategories)");
            params.put("reasonCategories", new ArrayList<>(categories));
        }

        if (!reasonTexts.isEmpty()) {
            parts.add("LOWER(TRIM(return_reason_remarks)) IN (:reasonTexts)");
            params.put("reasonTexts", new ArrayList<>(reasonTexts));
        }

        if (!parts.isEmpty()) {
            reportFilters.append(" AND (").append(String.join(" OR ", parts)).append(")");
        }
    }


    // Accepts codes or labels ("Pharmacy → Warehouse", "Pharmacy -> Warehouse");
    // ALL means no restriction.
    private List<String> parseCodes(List<String> requested, Set<String> allowed, String paramName) {

        if (isEmpty(requested)) {
            return List.of();
        }

        Set<String> codes = new LinkedHashSet<>();

        for (String value : requested) {
            if (!hasText(value)) {
                continue;
            }

            String code = normalizeCode(value);

            if ("ALL".equals(code)) {
                return List.of();
            }

            if (!allowed.contains(code)) {
                throw new IllegalArgumentException("Invalid " + paramName + ": " + value
                        + ". Allowed: ALL, " + String.join(", ", allowed));
            }
            codes.add(code);
        }

        return new ArrayList<>(codes);
    }


    // "Pharmacy → Warehouse" -> PHARMACY_TO_WAREHOUSE, "damage" -> DAMAGE.
    private static String normalizeCode(String value) {

        return value.trim()
                .toUpperCase()
                .replace("→", " TO ")
                .replace("->", " TO ")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }


    private StockReturnReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COALESCE(SUM(returned_quantity), 0),
                       COALESCE(SUM(returned_quantity) FILTER (WHERE reason_category = 'DAMAGE'), 0),
                       COALESCE(SUM(returned_quantity) FILTER (WHERE reason_category = 'EXPIRY'), 0)
                FROM report
                """, params).getSingleResult();

        StockReturnReportSummaryDto summary = new StockReturnReportSummaryDto();
        summary.setTotalReturnedQuantity(toLong(row[0]));
        summary.setDamageQuantity(toLong(row[1]));
        summary.setExpiryQuantity(toLong(row[2]));

        return summary;
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<StockReturnReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            StockReturnReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "returnDate", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // src_order and row_id as tie-breakers keep page boundaries stable
        // when several rows share the same sort value.
        Query query = nativeQuery(reportSql + """
                SELECT return_date, reference_number, return_type,
                       source_name, destination_name,
                       product_name, batch_number, expiry_date,
                       reason_category, returned_quantity
                FROM report
                ORDER BY %s %s NULLS LAST, src_order, row_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<StockReturnReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            StockReturnReportRowDto dto = new StockReturnReportRowDto();
            dto.setReturnDate(toLocalDate(r[0]));
            dto.setReferenceNumber((String) r[1]);
            dto.setReturnType((String) r[2]);
            dto.setReturnTypeLabel(RETURN_TYPES.get((String) r[2]));
            dto.setSourceName((String) r[3]);
            dto.setDestinationName((String) r[4]);
            dto.setProductName((String) r[5]);
            dto.setBatchNo((String) r[6]);
            dto.setExpiryDate(toLocalDate(r[7]));
            dto.setReturnReason((String) r[8]);
            dto.setReturnedQuantity(toLong(r[9]));
            rows.add(dto);
        }

        return rows;
    }


    private Query nativeQuery(String sql, Map<String, Object> params) {

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }


    private static List<String> lowerTrimmed(List<String> values) {
        return values.stream().map(v -> v.trim().toLowerCase()).toList();
    }


    // return_date comes back as a timestamp, expiry_date as a date.
    private static LocalDate toLocalDate(Object value) {

        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        return ((Date) value).toLocalDate();
    }


    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }


    // Treats user input as literal text inside ILIKE (no % / _ wildcards).
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }


    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
