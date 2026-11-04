package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportFilter;
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportPageDto;
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportResponse;
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportRowDto;
import tiameds.pharmabackend.dto.auditReports.StockAdjustmentReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.auditReports.StockAdjustmentReportService;
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
public class StockAdjustmentReportServiceImpl implements StockAdjustmentReportService {

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
            "Adjustment Date", "Pharmacy", "Reference Number", "Product Name", "Batch No", "Expiry",
            "Adjustment Type", "Adjustment Reason", "Stock Before", "Adjustment In", "Adjustment Out",
            "Net Adjustment", "Stock After", "Adjusted By");

    // Adjustment Type dropdown: code (as stored in adjustment_type) -> label.
    private static final Map<String, String> ADJUSTMENT_TYPES = new LinkedHashMap<>();

    static {
        ADJUSTMENT_TYPES.put("INCREASE", "Stock Increase");
        ADJUSTMENT_TYPES.put("DECREASE", "Stock Decrease");
    }

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("adjustmentDate", "adjustment_date"),
            Map.entry("pharmacyName", "pharmacy_name"),
            Map.entry("referenceNumber", "reference_number"),
            Map.entry("productName", "product_name"),
            Map.entry("batchNo", "batch_number"),
            Map.entry("expiryDate", "expiry_date"),
            Map.entry("adjustmentType", "adjustment_type"),
            Map.entry("adjustmentReason", "adjustment_reason"),
            Map.entry("stockBefore", "stock_before"),
            Map.entry("adjustmentIn", "adjustment_in"),
            Map.entry("adjustmentOut", "adjustment_out"),
            Map.entry("netAdjustment", "net_adjustment"),
            Map.entry("stockAfter", "stock_after"),
            Map.entry("adjustedBy", "adjusted_by_name"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public StockAdjustmentReportResponse getStockAdjustmentReport(
            StockAdjustmentReportFilter filter,
            UserDetails user) {

        ReportQuery report = prepareReport(filter, user);

        StockAdjustmentReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        long totalRecords = summary.getTotalAdjustments();

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

        StockAdjustmentReportPageDto details = new StockAdjustmentReportPageDto();
        details.setData(totalRecords == 0
                ? List.of()
                : fetchPage(report.sql(), report.params(), filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        StockAdjustmentReportResponse response = new StockAdjustmentReportResponse();
        response.setSummary(summary);
        response.setAdjustmentTransactions(details);
        response.setGeneratedBy(report.user().getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Runs the KPI totals (which carry the row count) now, so bad filters,
    // missing access and the Excel row limit fail before any of the file has
    // been sent.
    @Override
    public ReportExport exportStockAdjustmentReport(
            StockAdjustmentReportFilter filter,
            String format,
            UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        ReportQuery report = prepareReport(filter, user);

        StockAdjustmentReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        long totalRecords = summary.getTotalAdjustments();

        if (exportFormat == ExportFormat.XLSX && totalRecords > EXCEL_ROW_LIMIT) {
            throw new IllegalArgumentException(
                    "Too many rows for Excel (" + totalRecords + "). Narrow the filters or download CSV.");
        }

        String generatedBy = report.user().getFullName();
        LocalDateTime generatedOn = LocalDateTime.now();
        String pharmacies = pharmacyNames(report.pharmacyIds());

        boolean singleDay = report.fromDate().equals(report.toDate());
        String period = singleDay
                ? report.fromDate().format(DISPLAY_DATE)
                : report.fromDate().format(DISPLAY_DATE) + " to " + report.toDate().format(DISPLAY_DATE);
        // stock-adjustment_2026-10-08, stock-adjustment_2026-10-01_to_2026-10-08
        String fileName = "stock-adjustment_"
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

                    writer.title("Stock Adjustment Report");
                    writer.line("Pharmacy", pharmacies);
                    writer.line("Period", period);
                    writer.line("Generated By", generatedBy);
                    writer.line("Generated On", generatedOn);
                    writer.blank();

                    writer.line("Stock Increase Quantity", summary.getStockIncreaseQuantity());
                    writer.line("Stock Increase Adjustments", summary.getStockIncreaseCount());
                    writer.line("Stock Decrease Quantity", summary.getStockDecreaseQuantity());
                    writer.line("Stock Decrease Adjustments", summary.getStockDecreaseCount());
                    writer.line("Net Adjustment Quantity", summary.getNetAdjustmentQuantity());
                    writer.line("Total Adjustments", summary.getTotalAdjustments());
                    writer.blank();

                    writer.header(EXPORT_HEADERS);

                    for (long offset = 0; offset < totalRecords; offset += EXPORT_BATCH_SIZE) {

                        List<StockAdjustmentReportRowDto> batch =
                                fetchPage(report.sql(), report.params(), filter, EXPORT_BATCH_SIZE, offset);

                        for (StockAdjustmentReportRowDto row : batch) {
                            writer.row(Arrays.asList(
                                    row.getAdjustmentDate(), row.getPharmacyName(), row.getReferenceNumber(),
                                    row.getProductName(), row.getBatchNo(),
                                    row.getExpiryDate() == null ? null : row.getExpiryDate().format(EXPIRY_MONTH),
                                    row.getAdjustmentTypeLabel(), row.getAdjustmentReason(),
                                    row.getStockBefore(), row.getAdjustmentIn(), row.getAdjustmentOut(),
                                    row.getNetAdjustment(), row.getStockAfter(), row.getAdjustedByName()));
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
    // export: the user, the resolved period and pharmacies, and the CTE SQL
    // with its parameters.
    private record ReportQuery(
            UserDetails user,
            LocalDate fromDate,
            LocalDate toDate,
            List<String> pharmacyIds,
            String sql,
            Map<String, Object> params) {
    }


    private ReportQuery prepareReport(StockAdjustmentReportFilter filter, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate fromDate = filter.getFromDate() != null ? filter.getFromDate() : LocalDate.now();
        LocalDate toDate = filter.getToDate() != null ? filter.getToDate() : fromDate;

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate cannot be before fromDate.");
        }

        List<String> pharmacyIds = resolvePharmacyIds(filter.getPharmacyIds(), persistentUser);

        Map<String, Object> params = new HashMap<>();
        String reportSql = buildReportSql(filter, pharmacyIds, fromDate, toDate, params);

        return new ReportQuery(persistentUser, fromDate, toDate, pharmacyIds, reportSql, params);
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


    @Override
    @SuppressWarnings("unchecked")
    public List<String> getAdjustmentReasons(List<String> requestedPharmacyIds, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> pharmacyIds = resolvePharmacyIds(requestedPharmacyIds, persistentUser);

        // One spelling per reason (the first alphabetically) so "Damage" and
        // "damage " don't show up as two options.
        return entityManager.createNativeQuery("""
                        SELECT MIN(TRIM(stock_adjustment_reason)) AS reason
                        FROM pharma_stock_adjustment
                        WHERE pharmacy_id IN (:pharmacyIds)
                          AND NULLIF(TRIM(stock_adjustment_reason), '') IS NOT NULL
                        GROUP BY LOWER(TRIM(stock_adjustment_reason))
                        ORDER BY reason
                        """)
                .setParameter("pharmacyIds", pharmacyIds)
                .getResultList();
    }


    // Not sent = every accessible pharmacy; sent = those, if all accessible.
    private List<String> resolvePharmacyIds(List<String> requested, UserDetails user) {

        List<String> accessible = accessiblePharmacyIds(user);

        List<String> pharmacyIds = isEmpty(requested)
                ? accessible
                : checkAccess(requested, accessible);

        if (pharmacyIds.isEmpty()) {
            throw new AccessDeniedException("You do not have access to any pharmacy.");
        }

        return pharmacyIds;
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


    private List<String> checkAccess(List<String> requested, List<String> accessible) {

        List<String> denied = requested.stream()
                .filter(id -> !accessible.contains(id))
                .toList();

        if (!denied.isEmpty()) {
            throw new AccessDeniedException(
                    "You are not authorized to view pharmacy: " + String.join(", ", denied));
        }

        return requested.stream().distinct().toList();
    }


    /*
     * Builds the report as a CTE; callers wrap it with SELECT ... FROM report.
     *
     *   report - one row per pharmacy stock adjustment in the period, joined to
     *            product, batch, pharmacy, the user who recorded it and the
     *            supplier of the first purchase invoice the batch came in on,
     *            narrowed by every filter.
     *
     * actual_stock_qty is the counted stock the adjustment set (stock after)
     * and adjustment_qty the absolute change, so stock before is after - qty
     * for an INCREASE and after + qty for a DECREASE.
     */
    private String buildReportSql(
            StockAdjustmentReportFilter filter,
            List<String> pharmacyIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        params.put("pharmacyIds", pharmacyIds);
        params.put("fromTs", Timestamp.valueOf(fromDate.atStartOfDay()));
        params.put("toTs", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));

        // Each fragment starts with a space so it never glues onto the
        // previous one (a text block would drop that leading space).
        StringBuilder filters = new StringBuilder();

        if (!isEmpty(filter.getProductNames())) {
            filters.append(" AND LOWER(TRIM(p.product_name)) IN (:productNames)");
            params.put("productNames", lowerTrimmed(filter.getProductNames()));
        }

        if (!isEmpty(filter.getProductCategoryIds())) {
            filters.append(" AND p.product_category_id IN (:productCategoryIds)");
            params.put("productCategoryIds", filter.getProductCategoryIds());
        }

        if (!isEmpty(filter.getBrandNames())) {
            filters.append(" AND LOWER(TRIM(p.brand_name)) IN (:brandNames)");
            params.put("brandNames", lowerTrimmed(filter.getBrandNames()));
        }

        if (hasText(filter.getBatchNo())) {
            filters.append(" AND b.batch_number ILIKE :batchNo");
            params.put("batchNo", "%" + escapeLike(filter.getBatchNo().trim()) + "%");
        }

        if (!isEmpty(filter.getSupplierIds())) {
            filters.append(" AND sup.supplier_id IN (:supplierIds)");
            params.put("supplierIds", filter.getSupplierIds());
        }

        String adjustmentType = parseAdjustmentType(filter.getAdjustmentType());
        if (adjustmentType != null) {
            filters.append(" AND x.adjustment_type = :adjustmentType");
            params.put("adjustmentType", adjustmentType);
        }

        if (!isEmpty(filter.getAdjustmentReasons())) {
            filters.append(" AND LOWER(TRIM(x.stock_adjustment_reason)) IN (:adjustmentReasons)");
            params.put("adjustmentReasons", lowerTrimmed(filter.getAdjustmentReasons()));
        }

        if (hasText(filter.getReferenceNumber())) {
            filters.append(" AND x.stock_adjustment_no ILIKE :referenceNumber");
            params.put("referenceNumber", "%" + escapeLike(filter.getReferenceNumber().trim()) + "%");
        }

        if (!isEmpty(filter.getAdjustedByIds())) {
            filters.append(" AND x.created_by IN (:adjustedByIds)");
            params.put("adjustedByIds", filter.getAdjustedByIds());
        }

        return """
                WITH report AS (
                    SELECT x.stock_adjustment_id AS row_id,
                           COALESCE(x.stock_adjustment_date, x.created_at) AS adjustment_date,
                           ph.pharmacy_name,
                           x.stock_adjustment_no AS reference_number,
                           p.product_name,
                           b.batch_number,
                           b.expiry_date,
                           x.adjustment_type,
                           NULLIF(TRIM(x.stock_adjustment_reason), '') AS adjustment_reason,
                           CASE WHEN x.adjustment_type = 'INCREASE'
                                THEN x.actual_stock_qty - COALESCE(x.adjustment_qty, 0)
                                ELSE x.actual_stock_qty + COALESCE(x.adjustment_qty, 0)
                                END AS stock_before,
                           CASE WHEN x.adjustment_type = 'INCREASE'
                                THEN COALESCE(x.adjustment_qty, 0) ELSE 0 END AS adjustment_in,
                           CASE WHEN x.adjustment_type = 'DECREASE'
                                THEN COALESCE(x.adjustment_qty, 0) ELSE 0 END AS adjustment_out,
                           CASE WHEN x.adjustment_type = 'INCREASE'
                                THEN COALESCE(x.adjustment_qty, 0)
                                ELSE -COALESCE(x.adjustment_qty, 0)
                                END AS net_adjustment,
                           x.actual_stock_qty AS stock_after,
                           u.full_name AS adjusted_by_name
                    FROM pharma_stock_adjustment x
                    JOIN pharma_product_details p ON p.product_id = x.product_id
                    LEFT JOIN pharma_batch_details b ON b.batch_id = x.batch_id
                    LEFT JOIN pharma_pharmacy_details ph ON ph.pharmacy_id = x.pharmacy_id
                    LEFT JOIN pharma_user_details u ON u.user_id = x.created_by
                    LEFT JOIN LATERAL (
                        SELECT pu.supplier_id
                        FROM pharma_purchase_details pd
                        JOIN pharma_purchase pu ON pu.purchase_id = pd.purchase_id
                        WHERE pd.batch_id = x.batch_id
                        ORDER BY pd.purchase_details_id
                        LIMIT 1
                    ) sup ON TRUE
                    WHERE x.pharmacy_id IN (:pharmacyIds)
                      AND COALESCE(x.stock_adjustment_date, x.created_at) >= :fromTs
                      AND COALESCE(x.stock_adjustment_date, x.created_at) <  :toTs
                      %s
                )
                """.formatted(filters);
    }


    // Accepts codes or labels ("Stock Increase"); null, blank or ALL means no
    // restriction.
    private String parseAdjustmentType(String requested) {

        if (!hasText(requested)) {
            return null;
        }

        String code = requested.trim().toUpperCase().replace(' ', '_');

        if (code.startsWith("STOCK_")) {
            code = code.substring("STOCK_".length());
        }

        if ("ALL".equals(code)) {
            return null;
        }

        if (!ADJUSTMENT_TYPES.containsKey(code)) {
            throw new IllegalArgumentException("Invalid adjustmentType: " + requested
                    + ". Allowed: ALL, " + String.join(", ", ADJUSTMENT_TYPES.keySet()));
        }

        return code;
    }


    private StockAdjustmentReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COALESCE(SUM(adjustment_in), 0),
                       COUNT(*) FILTER (WHERE adjustment_type = 'INCREASE'),
                       COALESCE(SUM(adjustment_out), 0),
                       COUNT(*) FILTER (WHERE adjustment_type = 'DECREASE'),
                       COALESCE(SUM(net_adjustment), 0),
                       COUNT(*)
                FROM report
                """, params).getSingleResult();

        StockAdjustmentReportSummaryDto summary = new StockAdjustmentReportSummaryDto();
        summary.setStockIncreaseQuantity(toLong(row[0]));
        summary.setStockIncreaseCount(toLong(row[1]));
        summary.setStockDecreaseQuantity(toLong(row[2]));
        summary.setStockDecreaseCount(toLong(row[3]));
        summary.setNetAdjustmentQuantity(toLong(row[4]));
        summary.setTotalAdjustments(toLong(row[5]));

        return summary;
    }


    @SuppressWarnings("unchecked")
    private List<StockAdjustmentReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            StockAdjustmentReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "adjustmentDate", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // row_id as tie-breaker keeps page boundaries stable when several
        // rows share the same sort value.
        Query query = nativeQuery(reportSql + """
                SELECT adjustment_date, pharmacy_name, reference_number,
                       product_name, batch_number, expiry_date,
                       adjustment_type, adjustment_reason,
                       stock_before, adjustment_in, adjustment_out, net_adjustment, stock_after,
                       adjusted_by_name
                FROM report
                ORDER BY %s %s NULLS LAST, row_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<StockAdjustmentReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            StockAdjustmentReportRowDto dto = new StockAdjustmentReportRowDto();
            dto.setAdjustmentDate(toLocalDate(r[0]));
            dto.setPharmacyName((String) r[1]);
            dto.setReferenceNumber((String) r[2]);
            dto.setProductName((String) r[3]);
            dto.setBatchNo((String) r[4]);
            dto.setExpiryDate(toLocalDate(r[5]));
            dto.setAdjustmentType((String) r[6]);
            dto.setAdjustmentTypeLabel(ADJUSTMENT_TYPES.get((String) r[6]));
            dto.setAdjustmentReason((String) r[7]);
            dto.setStockBefore(toNullableLong(r[8]));
            dto.setAdjustmentIn(toLong(r[9]));
            dto.setAdjustmentOut(toLong(r[10]));
            dto.setNetAdjustment(toLong(r[11]));
            dto.setStockAfter(toNullableLong(r[12]));
            dto.setAdjustedByName((String) r[13]);
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


    // adjustment_date comes back as a timestamp, expiry_date as a date.
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


    private static Long toNullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
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
