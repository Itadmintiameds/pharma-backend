package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.StockDamageReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockDamageReportPageDto;
import tiameds.pharmabackend.dto.auditReports.StockDamageReportResponse;
import tiameds.pharmabackend.dto.auditReports.StockDamageReportRowDto;
import tiameds.pharmabackend.dto.auditReports.StockDamageReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;
import tiameds.pharmabackend.enums.StockReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.auditReports.StockDamageReportService;
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
public class StockDamageReportServiceImpl implements StockDamageReportService {

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
            "Transaction Date", "Product Name", "Batch No", "Expiry", "Transaction Type",
            "Reference Number", "Source", "Destination",
            "Returned Quantity", "Adjusted Quantity", "Total Damaged Quantity");

    // Transaction Type dropdown: code -> label. The codes are produced by the
    // txn CTE in buildReportSql.
    private static final Map<String, String> TRANSACTION_TYPES = new LinkedHashMap<>();

    static {
        TRANSACTION_TYPES.put("STOCK_RETURN", "Stock Return");
        TRANSACTION_TYPES.put("STOCK_ADJUSTMENT", "Stock Adjustment");
    }

    // Return / adjustment reasons are free text, so damage is recognised by
    // wording (case-insensitive POSIX regex).
    private static final String DAMAGE_REASON_PATTERN = "damag|broke|breakage";

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("transactionDate", "transaction_date"),
            Map.entry("productName", "product_name"),
            Map.entry("batchNo", "batch_number"),
            Map.entry("expiryDate", "expiry_date"),
            Map.entry("transactionType", "transaction_type"),
            Map.entry("referenceNumber", "reference_number"),
            Map.entry("source", "source_name"),
            Map.entry("destination", "destination_name"),
            Map.entry("returnedQuantity", "returned_quantity"),
            Map.entry("adjustedQuantity", "adjusted_quantity"),
            Map.entry("totalDamagedQuantity", "total_damaged_quantity"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public StockDamageReportResponse getStockDamageReport(
            StockDamageReportFilter filter,
            UserDetails user) {

        ReportQuery report = prepareReport(filter, user);

        StockDamageReportSummaryDto summary = fetchSummary(report.sql(), report.params());
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

        StockDamageReportPageDto details = new StockDamageReportPageDto();
        details.setData(fetchPage(report.sql(), report.params(), filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        StockDamageReportResponse response = new StockDamageReportResponse();
        response.setSummary(summary);
        response.setDamageTransactions(details);
        response.setGeneratedBy(report.user().getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Runs the count and the KPI totals now, so bad filters, missing access
    // and the Excel row limit fail before any of the file has been sent.
    @Override
    public ReportExport exportStockDamageReport(
            StockDamageReportFilter filter,
            String format,
            UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        ReportQuery report = prepareReport(filter, user);

        long totalRecords = fetchTotalRecords(report.sql(), report.params());

        if (exportFormat == ExportFormat.XLSX && totalRecords > EXCEL_ROW_LIMIT) {
            throw new IllegalArgumentException(
                    "Too many rows for Excel (" + totalRecords + "). Narrow the filters or download CSV.");
        }

        StockDamageReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        String generatedBy = report.user().getFullName();
        LocalDateTime generatedOn = LocalDateTime.now();
        String pharmacies = pharmacyNames(report.pharmacyIds());
        String warehouses = warehouseNames(report.warehouseIds());

        boolean singleDay = report.fromDate().equals(report.toDate());
        String period = singleDay
                ? report.fromDate().format(DISPLAY_DATE)
                : report.fromDate().format(DISPLAY_DATE) + " to " + report.toDate().format(DISPLAY_DATE);
        // stock-damage_2026-10-08, stock-damage_2026-10-01_to_2026-10-08
        String fileName = "stock-damage_"
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

                    writer.title("Stock Damage Report");
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

                    writer.line("Returned Quantity", summary.getReturnedQuantity());
                    writer.line("Adjusted Quantity", summary.getAdjustedQuantity());
                    writer.line("Total Damaged Quantity", summary.getTotalDamagedQuantity());
                    writer.blank();

                    writer.header(EXPORT_HEADERS);

                    for (long offset = 0; offset < totalRecords; offset += EXPORT_BATCH_SIZE) {

                        List<StockDamageReportRowDto> batch =
                                fetchPage(report.sql(), report.params(), filter, EXPORT_BATCH_SIZE, offset);

                        for (StockDamageReportRowDto row : batch) {
                            writer.row(Arrays.asList(
                                    row.getTransactionDate(), row.getProductName(), row.getBatchNo(),
                                    row.getExpiryDate() == null ? null : row.getExpiryDate().format(EXPIRY_MONTH),
                                    row.getTransactionTypeLabel(), row.getReferenceNumber(),
                                    row.getSourceName(), row.getDestinationName(),
                                    row.getReturnedQuantity(), row.getAdjustedQuantity(),
                                    row.getTotalDamagedQuantity()));
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


    private ReportQuery prepareReport(StockDamageReportFilter filter, UserDetails user) {

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
     *   txn    - one row per damaged return line / adjustment in the period:
     *              STOCK_RETURN     warehouse returns (pharmacy -> warehouse) that
     *                               have been sent (not Draft, not deleted) and carry
     *                               damaged stock: the line was raised from a damaged
     *                               distribution line, or the return type / line
     *                               reason mentions damage. Expired and ordinary
     *                               ("Pharmacy Inventory") returns are left out;
     *                               plus confirmed purchase returns (-> supplier)
     *                               whose line reason mentions damage
     *              STOCK_ADJUSTMENT DECREASE adjustments whose reason or remarks
     *                               mention damage (stock written off)
     *            A return is in scope through its source or its destination
     *            location; an adjustment through its own location.
     *   base   - txn joined to product, batch, location / supplier names and the
     *            supplier of the first purchase invoice the batch came in on,
     *            narrowed by the product filters
     *   report - base narrowed by supplier, transaction type, reference number
     *            and destination
     */
    private String buildReportSql(
            StockDamageReportFilter filter,
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

        if (!isEmpty(filter.getProductIds())) {
            productFilters.append(" AND p.product_id IN (:productIds)");
            params.put("productIds", filter.getProductIds());
        }

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

        List<String> transactionTypes = parseTransactionTypes(filter.getTransactionTypes());
        if (!transactionTypes.isEmpty()) {
            reportFilters.append(" AND transaction_type IN (:transactionTypes)");
            params.put("transactionTypes", transactionTypes);
        }

        if (hasText(filter.getReferenceNumber())) {
            reportFilters.append(" AND reference_number ILIKE :referenceNumber");
            params.put("referenceNumber", "%" + escapeLike(filter.getReferenceNumber().trim()) + "%");
        }

        List<String> destinationParts = new ArrayList<>();

        if (!isEmpty(filter.getDestinationWarehouseIds())) {
            destinationParts.add("(destination_type = 'WAREHOUSE' AND destination_id IN (:destinationWarehouseIds))");
            params.put("destinationWarehouseIds", filter.getDestinationWarehouseIds());
        }

        if (!isEmpty(filter.getDestinationSupplierIds())) {
            // destination_id is text (it also holds warehouse ids).
            destinationParts.add("(destination_type = 'SUPPLIER' AND destination_id IN (:destinationSupplierIds))");
            params.put("destinationSupplierIds",
                    filter.getDestinationSupplierIds().stream().map(String::valueOf).toList());
        }

        if (!destinationParts.isEmpty()) {
            reportFilters.append(" AND (").append(String.join(" OR ", destinationParts)).append(")");
        }

        return """
                WITH txn AS (
                    SELECT 'STOCK_RETURN' AS transaction_type,
                           1 AS src_order, x.warehouse_return_id AS doc_id,
                           d.warehouse_return_detail_id AS row_id,
                           x.stock_return_date AS transaction_date,
                           x.stock_return_no AS reference_number,
                           d.product_id, d.batch_id,
                           'PHARMACY' AS source_type, x.from_pharmacy_id AS source_id,
                           CAST('WAREHOUSE' AS VARCHAR) AS destination_type,
                           CAST(x.to_warehouse_id AS VARCHAR) AS destination_id,
                           COALESCE(d.dispatch_quantity, d.return_quantity, 0) AS returned_quantity,
                           CAST(0 AS BIGINT) AS adjusted_quantity
                    FROM pharma_warehouse_return x
                    JOIN pharma_warehouse_return_details d
                      ON d.warehouse_return_id = x.warehouse_return_id
                    WHERE COALESCE(x.is_delete, FALSE) = FALSE
                      AND x.stock_return_status <> :draftReturnStatus
                      AND (d.warehouse_distribution_details_id IS NOT NULL
                        OR x.stock_return_type ~* :damagePattern
                        OR d.return_reason ~* :damagePattern)
                      AND x.stock_return_date >= :fromTs
                      AND x.stock_return_date <  :toTs
                      AND (%1$s)

                    UNION ALL

                    SELECT 'STOCK_RETURN',
                           2, x.purchase_return_id,
                           d.purchase_return_detail_id,
                           x.purchase_return_date,
                           x.return_no,
                           d.product_id, d.batch_id,
                           CASE WHEN x.warehouse_id IS NOT NULL THEN 'WAREHOUSE' ELSE 'PHARMACY' END,
                           COALESCE(x.warehouse_id, x.pharmacy_id),
                           'SUPPLIER',
                           CAST(pu.supplier_id AS VARCHAR),
                           COALESCE(d.purchase_return_quantity, 0) + COALESCE(d.free_return_quantity, 0),
                           0
                    FROM pharma_purchase_return x
                    JOIN pharma_purchase_return_details d
                      ON d.purchase_return_id = x.purchase_return_id
                    LEFT JOIN pharma_purchase pu ON pu.purchase_id = x.purchase_id
                    WHERE x.status = :confirmedPurchaseReturn
                      AND COALESCE(x.is_draft_deleted, FALSE) = FALSE
                      AND COALESCE(d.is_active, TRUE) = TRUE
                      AND d.return_reason ~* :damagePattern
                      AND x.purchase_return_date >= :fromTs
                      AND x.purchase_return_date <  :toTs
                      AND (%2$s)

                    UNION ALL

                    SELECT 'STOCK_ADJUSTMENT',
                           3, x.stock_adjustment_id,
                           x.stock_adjustment_id,
                           COALESCE(x.stock_adjustment_date, x.created_at),
                           x.stock_adjustment_no,
                           x.product_id, x.batch_id,
                           CASE WHEN x.warehouse_id IS NOT NULL THEN 'WAREHOUSE' ELSE 'PHARMACY' END,
                           COALESCE(x.warehouse_id, x.pharmacy_id),
                           NULL,
                           NULL,
                           0,
                           COALESCE(x.adjustment_qty, 0)
                    FROM pharma_stock_adjustment x
                    WHERE x.adjustment_type = 'DECREASE'
                      AND (x.stock_adjustment_reason ~* :damagePattern
                        OR x.stock_adjustment_remarks ~* :damagePattern)
                      AND COALESCE(x.stock_adjustment_date, x.created_at) >= :fromTs
                      AND COALESCE(x.stock_adjustment_date, x.created_at) <  :toTs
                      AND (%2$s)
                ),
                base AS (
                    SELECT t.*,
                           CASE WHEN t.source_type = 'WAREHOUSE' THEN swh.warehouse_name
                                ELSE sph.pharmacy_name END AS source_name,
                           CASE WHEN t.destination_type = 'WAREHOUSE' THEN dwh.warehouse_name
                                WHEN t.destination_type = 'SUPPLIER'  THEN dsup.supplier_name
                                END AS destination_name,
                           p.product_name,
                           b.batch_number,
                           b.expiry_date,
                           sup.supplier_id,
                           t.returned_quantity + t.adjusted_quantity AS total_damaged_quantity
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
                           ON t.destination_type = 'SUPPLIER'
                          AND CAST(dsup.supplier_id AS VARCHAR) = t.destination_id
                    LEFT JOIN LATERAL (
                        SELECT sm.supplier_id
                        FROM pharma_purchase_details pd
                        JOIN pharma_purchase pu ON pu.purchase_id = pd.purchase_id
                        JOIN pharma_supplier_master sm ON sm.supplier_id = pu.supplier_id
                        WHERE pd.batch_id = t.batch_id
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


    // Accepts codes or labels ("Stock Return"); ALL means no restriction.
    private List<String> parseTransactionTypes(List<String> requested) {

        if (isEmpty(requested)) {
            return List.of();
        }

        Set<String> codes = new LinkedHashSet<>();

        for (String value : requested) {
            if (!hasText(value)) {
                continue;
            }

            String code = value.trim().toUpperCase().replace(' ', '_');

            if ("ALL".equals(code)) {
                return List.of();
            }

            if (!TRANSACTION_TYPES.containsKey(code)) {
                throw new IllegalArgumentException("Invalid transactionType: " + value
                        + ". Allowed: ALL, " + String.join(", ", TRANSACTION_TYPES.keySet()));
            }
            codes.add(code);
        }

        return new ArrayList<>(codes);
    }


    private StockDamageReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COALESCE(SUM(returned_quantity), 0),
                       COALESCE(SUM(adjusted_quantity), 0),
                       COALESCE(SUM(total_damaged_quantity), 0)
                FROM report
                """, params).getSingleResult();

        StockDamageReportSummaryDto summary = new StockDamageReportSummaryDto();
        summary.setReturnedQuantity(toLong(row[0]));
        summary.setAdjustedQuantity(toLong(row[1]));
        summary.setTotalDamagedQuantity(toLong(row[2]));

        return summary;
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<StockDamageReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            StockDamageReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "transactionDate", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // src_order and row_id as tie-breakers keep page boundaries stable
        // when several rows share the same sort value.
        Query query = nativeQuery(reportSql + """
                SELECT transaction_date, product_name, batch_number, expiry_date,
                       transaction_type, reference_number, source_name, destination_name,
                       returned_quantity, adjusted_quantity, total_damaged_quantity
                FROM report
                ORDER BY %s %s NULLS LAST, src_order, row_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<StockDamageReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            StockDamageReportRowDto dto = new StockDamageReportRowDto();
            dto.setTransactionDate(toLocalDate(r[0]));
            dto.setProductName((String) r[1]);
            dto.setBatchNo((String) r[2]);
            dto.setExpiryDate(toLocalDate(r[3]));
            dto.setTransactionType((String) r[4]);
            dto.setTransactionTypeLabel(TRANSACTION_TYPES.get((String) r[4]));
            dto.setReferenceNumber((String) r[5]);
            dto.setSourceName((String) r[6]);
            dto.setDestinationName((String) r[7]);
            dto.setReturnedQuantity(toLong(r[8]));
            dto.setAdjustedQuantity(toLong(r[9]));
            dto.setTotalDamagedQuantity(toLong(r[10]));
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


    // transaction_date comes back as a timestamp, expiry_date as a date.
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
