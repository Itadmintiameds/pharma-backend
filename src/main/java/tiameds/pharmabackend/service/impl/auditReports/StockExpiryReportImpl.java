package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.StockExpiryReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockExpiryReportPageDto;
import tiameds.pharmabackend.dto.auditReports.StockExpiryReportResponse;
import tiameds.pharmabackend.dto.auditReports.StockExpiryReportRowDto;
import tiameds.pharmabackend.dto.auditReports.StockExpiryReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.StockReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.auditReports.StockExpiryReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;
import tiameds.pharmabackend.service.impl.financialReport.export.ExportFormat;
import tiameds.pharmabackend.service.impl.financialReport.export.ReportTableWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockExpiryReportImpl implements StockExpiryReportService {

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

    private static final List<String> EXPORT_HEADERS = List.of(
            "Product Name", "Pharmacy / Warehouse", "Location Type", "Batch No", "Supplier Name",
            "Expiry Date", "Days to Expiry", "Expiry Status", "Stock Quantity", "Damaged Quantity");

    // Expiry Status column of the export, as the screen's badges word it.
    private static final Map<String, String> EXPIRY_STATUS_LABELS = Map.of(
            "EXPIRED", "Expired",
            "EXPIRING_THIS_MONTH", "Expiring This Month",
            "EXPIRING_SOON", "Expiring Soon",
            "VALID", "Valid");

    // "Expiring Soon" = expires after this month but within this many months
    // from today.
    private static final int EXPIRING_SOON_MONTHS = 3;

    private static final Set<String> EXPIRY_STATUSES = new LinkedHashSet<>(List.of(
            "ALL", "EXPIRED", "EXPIRING_THIS_MONTH", "EXPIRING_SOON", "VALID"));

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "locationName", "location_name",
            "productName", "product_name",
            "batchNo", "batch_number",
            "supplierName", "supplier_name",
            "expiryDate", "expiry_date",
            "monthsToExpiry", "days_to_expiry",
            "quantity", "quantity",
            "damagedQuantity", "damaged_quantity");

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public StockExpiryReportResponse getStockExpiryReport(
            StockExpiryReportFilter filter,
            UserDetails user) {

        ReportQuery report = prepareReport(filter, user);

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

        StockExpiryReportPageDto details = new StockExpiryReportPageDto();
        details.setData(fetchPage(report.sql(), report.params(), filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        StockExpiryReportResponse response = new StockExpiryReportResponse();
        response.setSummary(fetchSummary(report.sql(), report.params()));
        response.setExpiryDetails(details);
        response.setGeneratedBy(report.user().getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Runs the count and the KPI totals now, so bad filters, missing access
    // and the Excel row limit fail before any of the file has been sent.
    @Override
    public ReportExport exportStockExpiryReport(
            StockExpiryReportFilter filter,
            String format,
            UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        ReportQuery report = prepareReport(filter, user);

        long totalRecords = fetchTotalRecords(report.sql(), report.params());

        if (exportFormat == ExportFormat.XLSX && totalRecords > EXCEL_ROW_LIMIT) {
            throw new IllegalArgumentException(
                    "Too many rows for Excel (" + totalRecords + "). Narrow the filters or download CSV.");
        }

        StockExpiryReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        String generatedBy = report.user().getFullName();
        LocalDateTime generatedOn = LocalDateTime.now();
        String pharmacies = pharmacyNames(report.pharmacyIds());
        String warehouses = warehouseNames(report.warehouseIds());
        // stock-expiry_2026-10-08
        String fileName = "stock-expiry_" + report.asOfDate() + exportFormat.extension();

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

                    writer.title("Stock Expiry Report");
                    if (!pharmacies.isEmpty()) {
                        writer.line("Pharmacy", pharmacies);
                    }
                    if (!warehouses.isEmpty()) {
                        writer.line("Warehouse", warehouses);
                    }
                    writer.line("As-of Date", report.asOfDate().format(DISPLAY_DATE));
                    writer.line("Generated By", generatedBy);
                    writer.line("Generated On", generatedOn);
                    writer.blank();

                    writer.line("Expired Stock", summary.getExpiredStock());
                    writer.line("Expired Batches", summary.getExpiredBatches());
                    writer.line("Expiring This Month Quantity", summary.getExpiringThisMonthQuantity());
                    writer.line("Expiring This Month Batches", summary.getExpiringThisMonthBatches());
                    writer.line("Expiring Soon Quantity", summary.getExpiringSoonQuantity());
                    writer.line("Expiring Soon Batches", summary.getExpiringSoonBatches());
                    writer.line("Total Stock", summary.getTotalStock());
                    writer.line("Total Stock Batches", summary.getTotalStockBatches());
                    writer.blank();

                    writer.header(EXPORT_HEADERS);

                    for (long offset = 0; offset < totalRecords; offset += EXPORT_BATCH_SIZE) {

                        List<StockExpiryReportRowDto> batch =
                                fetchPage(report.sql(), report.params(), filter, EXPORT_BATCH_SIZE, offset);

                        for (StockExpiryReportRowDto row : batch) {
                            writer.row(Arrays.asList(
                                    row.getProductName(), row.getLocationName(), row.getLocationType(),
                                    row.getBatchNo(), row.getSupplierName(), row.getExpiryDate(),
                                    // Named monthsToExpiry, but it holds days (negative once expired).
                                    row.getMonthsToExpiry(),
                                    EXPIRY_STATUS_LABELS.getOrDefault(row.getExpiryStatus(), row.getExpiryStatus()),
                                    row.getQuantity(), row.getDamagedQuantity()));
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
    // export: the user, the as-of date, the resolved locations, and the CTE
    // SQL with its parameters.
    private record ReportQuery(
            UserDetails user,
            LocalDate asOfDate,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            String sql,
            Map<String, Object> params) {
    }


    private ReportQuery prepareReport(StockExpiryReportFilter filter, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        validate(filter);

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
        LocalDate asOfDate = filter.getAsOfDate() != null ? filter.getAsOfDate() : LocalDate.now();
        String reportSql = buildReportSql(filter, pharmacyIds, warehouseIds, asOfDate, params);

        return new ReportQuery(persistentUser, asOfDate, pharmacyIds, warehouseIds, reportSql, params);
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


    private void validate(StockExpiryReportFilter filter) {

        if (filter.getExpiryWithinMonths() != null && filter.getExpiryWithinDays() != null) {
            throw new IllegalArgumentException("Send either expiryWithinMonths or expiryWithinDays, not both.");
        }

        if (filter.getExpiryWithinMonths() != null && filter.getExpiryWithinMonths() < 1) {
            throw new IllegalArgumentException("expiryWithinMonths must be at least 1.");
        }

        if (filter.getExpiryWithinDays() != null && filter.getExpiryWithinDays() < 1) {
            throw new IllegalArgumentException("expiryWithinDays must be at least 1.");
        }

        if (filter.getExpiryFromMonth() != null && filter.getExpiryToMonth() != null
                && filter.getExpiryToMonth().isBefore(filter.getExpiryFromMonth())) {
            throw new IllegalArgumentException("expiryToMonth cannot be before expiryFromMonth.");
        }
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
     * "today" is the requested asOfDate; stock is always what is on hand now.
     *
     *   inv     - units on hand right now per location / product / batch
     *             (packagings of a batch are summed)
     *   damaged - damaged units reported when a distribution was received at
     *             the location and not yet sent back on a warehouse return.
     *             These never enter inventory, so they are reported alongside.
     *   stock   - inv and damaged side by side; rows with neither are dropped
     *   base    - stock joined to product, batch and the supplier of the first
     *            purchase invoice the batch came in on, with its expiry status:
     *              EXPIRED             expiry before today
     *              EXPIRING_THIS_MONTH expires today .. end of this month
     *              EXPIRING_SOON       after this month, within EXPIRING_SOON_MONTHS
     *              VALID               later than that
     *            Batches without an expiry date are left out.
     *   report - base narrowed by supplier and the Expiry Status dropdown
     */
    private String buildReportSql(
            StockExpiryReportFilter filter,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            LocalDate today,
            Map<String, Object> params) {

        params.put("today", Date.valueOf(today));
        params.put("nextMonthStart", Date.valueOf(today.withDayOfMonth(1).plusMonths(1)));
        params.put("soonEnd", Date.valueOf(today.plusMonths(EXPIRING_SOON_MONTHS)));

        // An empty list in IN (...) is invalid SQL, so each location kind is
        // only included when it has ids.
        List<String> invParts = new ArrayList<>();
        List<String> damagedLocations = new ArrayList<>();

        if (!pharmacyIds.isEmpty()) {
            params.put("pharmacyIds", pharmacyIds);
            invParts.add("""
                    SELECT 'PHARMACY' AS location_type, i.pharmacy_id AS location_id,
                           i.product_id, i.batch_id, SUM(i.total_stock) AS qty
                    FROM pharma_inventory i
                    WHERE i.pharmacy_id IN (:pharmacyIds)
                      AND i.batch_id IS NOT NULL
                    GROUP BY i.pharmacy_id, i.product_id, i.batch_id
                    """);
            damagedLocations.add("(w.destination_type = 'PHARMACY' AND w.destination_id IN (:pharmacyIds))");
        }

        if (!warehouseIds.isEmpty()) {
            params.put("warehouseIds", warehouseIds);
            invParts.add("""
                    SELECT 'WAREHOUSE' AS location_type, i.warehouse_id AS location_id,
                           i.product_id, i.batch_id, SUM(i.total_stock) AS qty
                    FROM pharma_warehouse_inventory i
                    WHERE i.warehouse_id IN (:warehouseIds)
                      AND i.batch_id IS NOT NULL
                    GROUP BY i.warehouse_id, i.product_id, i.batch_id
                    """);
            damagedLocations.add("(w.destination_type = 'WAREHOUSE' AND w.destination_id IN (:warehouseIds))");
        }

        params.put("draftReturnStatus", StockReturnStatus.DRAFT.getLabel());

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

        // Expiry Within only looks ahead, so already expired batches drop out.
        LocalDate withinEnd = null;
        if (filter.getExpiryWithinMonths() != null) {
            withinEnd = today.plusMonths(filter.getExpiryWithinMonths());
        } else if (filter.getExpiryWithinDays() != null) {
            withinEnd = today.plusDays(filter.getExpiryWithinDays());
        }

        if (withinEnd != null) {
            productFilters.append(" AND b.expiry_date >= :today AND b.expiry_date <= :withinEnd");
            params.put("withinEnd", Date.valueOf(withinEnd));
        }

        if (filter.getExpiryFromMonth() != null) {
            productFilters.append(" AND b.expiry_date >= :expiryFromDate");
            params.put("expiryFromDate", Date.valueOf(filter.getExpiryFromMonth().atDay(1)));
        }

        if (filter.getExpiryToMonth() != null) {
            productFilters.append(" AND b.expiry_date <= :expiryToDate");
            params.put("expiryToDate", Date.valueOf(filter.getExpiryToMonth().atEndOfMonth()));
        }

        StringBuilder reportFilters = new StringBuilder();

        if (!isEmpty(filter.getSupplierIds())) {
            reportFilters.append(" AND supplier_id IN (:supplierIds)");
            params.put("supplierIds", filter.getSupplierIds());
        }

        String expiryStatus = hasText(filter.getExpiryStatus())
                ? filter.getExpiryStatus().trim().toUpperCase().replace(' ', '_')
                : "ALL";

        if (!EXPIRY_STATUSES.contains(expiryStatus)) {
            throw new IllegalArgumentException("Invalid expiryStatus: " + filter.getExpiryStatus()
                    + ". Allowed: " + String.join(", ", EXPIRY_STATUSES));
        }

        if (!"ALL".equals(expiryStatus)) {
            reportFilters.append(" AND expiry_status = :expiryStatus");
            params.put("expiryStatus", expiryStatus);
        } else if (withinEnd == null
                && filter.getExpiryFromMonth() == null
                && filter.getExpiryToMonth() == null) {
            // With no Expiry Within or expiry month range to set the scope, "All"
            // means every batch that needs attention as of the day: expired,
            // expiring this month or expiring soon. Batches still valid beyond
            // that only show when VALID is picked explicitly.
            reportFilters.append(" AND expiry_status <> 'VALID'");
        }

        return """
                WITH inv AS (
                %1$s
                ),
                damaged AS (
                    SELECT w.destination_type AS location_type, w.destination_id AS location_id,
                           d.product_id, d.batch_id, SUM(d.damaged_quantity) AS qty
                    FROM pharma_warehouse_distribution_details d
                    JOIN pharma_warehouse_distribution w
                      ON w.warehouse_distribution_id = d.warehouse_distribution_id
                    WHERE d.damaged_quantity > 0
                      AND d.batch_id IS NOT NULL
                      AND (%2$s)
                      AND EXISTS (
                          SELECT 1
                          FROM pharma_warehouse_distribution_status st
                          WHERE st.warehouse_distribution_id = w.warehouse_distribution_id
                            AND st.warehouse_distribution_status = 'STOCK_RECEIVED'
                      )
                      AND NOT EXISTS (
                          SELECT 1
                          FROM pharma_warehouse_return_details rd
                          JOIN pharma_warehouse_return r
                            ON r.warehouse_return_id = rd.warehouse_return_id
                          WHERE rd.warehouse_distribution_details_id = d.warehouse_distribution_details_id
                            AND COALESCE(r.is_delete, FALSE) = FALSE
                            AND r.stock_return_status <> :draftReturnStatus
                      )
                    GROUP BY w.destination_type, w.destination_id, d.product_id, d.batch_id
                ),
                stock AS (
                    SELECT k.location_type, k.location_id, k.product_id, k.batch_id,
                           COALESCE(inv.qty, 0) AS qty,
                           COALESCE(dm.qty, 0)  AS damaged_qty
                    FROM (SELECT location_type, location_id, product_id, batch_id FROM inv
                          UNION
                          SELECT location_type, location_id, product_id, batch_id FROM damaged) k
                    LEFT JOIN inv
                           ON inv.location_type = k.location_type AND inv.location_id = k.location_id
                          AND inv.product_id = k.product_id AND inv.batch_id = k.batch_id
                    LEFT JOIN damaged dm
                           ON dm.location_type = k.location_type AND dm.location_id = k.location_id
                          AND dm.product_id = k.product_id AND dm.batch_id = k.batch_id
                    WHERE COALESCE(inv.qty, 0) > 0 OR COALESCE(dm.qty, 0) > 0
                ),
                base AS (
                    SELECT s.location_type,
                           s.location_id,
                           COALESCE(ph.pharmacy_name, wh.warehouse_name) AS location_name,
                           p.product_id,
                           p.product_name,
                           s.batch_id,
                           b.batch_number,
                           sup.supplier_id,
                           sup.supplier_name,
                           b.expiry_date,
                           b.expiry_date - :today AS days_to_expiry,
                           CASE WHEN b.expiry_date < :today          THEN 'EXPIRED'
                                WHEN b.expiry_date < :nextMonthStart THEN 'EXPIRING_THIS_MONTH'
                                WHEN b.expiry_date <= :soonEnd       THEN 'EXPIRING_SOON'
                                ELSE 'VALID' END AS expiry_status,
                           s.qty         AS quantity,
                           s.damaged_qty AS damaged_quantity
                    FROM stock s
                    JOIN pharma_product_details p ON p.product_id = s.product_id
                    JOIN pharma_batch_details b ON b.batch_id = s.batch_id
                    LEFT JOIN pharma_pharmacy_details ph
                           ON s.location_type = 'PHARMACY' AND ph.pharmacy_id = s.location_id
                    LEFT JOIN pharma_warehouse wh
                           ON s.location_type = 'WAREHOUSE' AND wh.warehouse_id = s.location_id
                    LEFT JOIN LATERAL (
                        SELECT sm.supplier_id, sm.supplier_name
                        FROM pharma_purchase_details pd
                        JOIN pharma_purchase pu ON pu.purchase_id = pd.purchase_id
                        JOIN pharma_supplier_master sm ON sm.supplier_id = pu.supplier_id
                        WHERE pd.batch_id = s.batch_id
                        ORDER BY pd.purchase_details_id
                        LIMIT 1
                    ) sup ON TRUE
                    WHERE b.expiry_date IS NOT NULL
                      %3$s
                ),
                report AS (
                    SELECT * FROM base WHERE 1 = 1 %4$s
                )
                """.formatted(
                String.join("UNION ALL\n", invParts),
                String.join(" OR ", damagedLocations),
                productFilters,
                reportFilters);
    }


    private StockExpiryReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COALESCE(SUM(quantity) FILTER (WHERE expiry_status = 'EXPIRED'), 0),
                       COUNT(*) FILTER (WHERE expiry_status = 'EXPIRED'),
                       COALESCE(SUM(quantity) FILTER (WHERE expiry_status = 'EXPIRING_THIS_MONTH'), 0),
                       COUNT(*) FILTER (WHERE expiry_status = 'EXPIRING_THIS_MONTH'),
                       COALESCE(SUM(quantity) FILTER (WHERE expiry_status = 'EXPIRING_SOON'), 0),
                       COUNT(*) FILTER (WHERE expiry_status = 'EXPIRING_SOON'),
                       COALESCE(SUM(quantity), 0),
                       COUNT(*)
                FROM report
                """, params).getSingleResult();

        StockExpiryReportSummaryDto summary = new StockExpiryReportSummaryDto();
        summary.setExpiredStock(toLong(row[0]));
        summary.setExpiredBatches(toLong(row[1]));
        summary.setExpiringThisMonthQuantity(toLong(row[2]));
        summary.setExpiringThisMonthBatches(toLong(row[3]));
        summary.setExpiringSoonQuantity(toLong(row[4]));
        summary.setExpiringSoonBatches(toLong(row[5]));
        summary.setTotalStock(toLong(row[6]));
        summary.setTotalStockBatches(toLong(row[7]));

        return summary;
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<StockExpiryReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            StockExpiryReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "expiryDate", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "desc".equalsIgnoreCase(filter.getSortDir()) ? "DESC" : "ASC";

        // location, product and batch as tie-breakers keep page boundaries
        // stable when several rows share the same sort value.
        Query query = nativeQuery(reportSql + """
                SELECT location_type, location_name, product_name, batch_number, supplier_name,
                       expiry_date, days_to_expiry, expiry_status, quantity, damaged_quantity
                FROM report
                ORDER BY %s %s NULLS LAST, location_id, product_id, batch_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<StockExpiryReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            StockExpiryReportRowDto dto = new StockExpiryReportRowDto();
            dto.setLocationType((String) r[0]);
            dto.setLocationName((String) r[1]);
            dto.setProductName((String) r[2]);
            dto.setBatchNo((String) r[3]);
            dto.setSupplierName((String) r[4]);
            dto.setExpiryDate(toLocalDate(r[5]));
            dto.setMonthsToExpiry(toLong(r[6]));
            dto.setExpiryStatus((String) r[7]);
            dto.setQuantity(toLong(r[8]));
            dto.setDamagedQuantity(toLong(r[9]));
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


    private static LocalDate toLocalDate(Object value) {

        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
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
