package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportFilter;
import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportPageDto;
import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportResponse;
import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportRowDto;
import tiameds.pharmabackend.dto.auditReports.PurchaseRegisterReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.auditReports.PurchaseRegisterReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;
import tiameds.pharmabackend.service.impl.financialReport.export.ExportFormat;
import tiameds.pharmabackend.service.impl.financialReport.export.ReportTableWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRegisterReportServiceImpl implements PurchaseRegisterReportService {

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
            "Purchase Date", "Invoice Number", "Supplier", "Pharmacy / Warehouse",
            "Product Name", "Batch No", "Expiry",
            "Purchased Quantity", "Free Quantity", "Purchase Rate",
            "Gross Amount", "Discount", "Taxable Amount", "GST %", "GST Amount", "Total Amount");

    // Expiry Month/Year: the UI format first, ISO as a fallback.
    private static final List<DateTimeFormatter> EXPIRY_FORMATS = List.of(
            DateTimeFormatter.ofPattern("MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM"));

    // Numeric GST rate of an invoice line: gst_percentage is text ("12%",
    // "Exempted"), so it is reduced to its digits; Exempted counts as 0.
    private static final String GST_RATE_SQL =
            "COALESCE(CAST(NULLIF(regexp_replace(pd.gst_percentage, '[^0-9.]', '', 'g'), '') AS NUMERIC), 0)";

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("purchaseDate", "purchase_date"),
            Map.entry("invoiceNumber", "invoice_no"),
            Map.entry("supplierName", "supplier_name"),
            Map.entry("locationName", "location_name"),            Map.entry("productName", "product_name"),
            Map.entry("batchNo", "batch_number"),
            Map.entry("expiryDate", "expiry_date"),
            Map.entry("purchasedQuantity", "purchased_quantity"),
            Map.entry("freeQuantity", "free_quantity"),
            Map.entry("purchaseRate", "purchase_rate"),
            Map.entry("grossAmount", "gross_amount"),
            Map.entry("discount", "discount"),
            Map.entry("taxableAmount", "taxable_amount"),
            Map.entry("gstPercentage", "gst_rate"),
            Map.entry("gstAmount", "gst_amount"),
            Map.entry("totalAmount", "total_amount"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public PurchaseRegisterReportResponse getPurchaseRegisterReport(
            PurchaseRegisterReportFilter filter,
            UserDetails user) {

        ReportQuery report = prepareReport(filter, user);

        PurchaseRegisterReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        long totalRecords = summary.getLineItemCount();

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

        PurchaseRegisterReportPageDto details = new PurchaseRegisterReportPageDto();
        details.setData(totalRecords == 0
                ? List.of()
                : fetchPage(report.sql(), report.params(), filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        PurchaseRegisterReportResponse response = new PurchaseRegisterReportResponse();
        response.setSummary(summary);
        response.setPurchaseDetails(details);
        response.setGeneratedBy(report.user().getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Runs the KPI totals (which carry the row count) now, so bad filters,
    // missing access and the Excel row limit fail before any of the file has
    // been sent.
    @Override
    public ReportExport exportPurchaseRegisterReport(
            PurchaseRegisterReportFilter filter,
            String format,
            UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        ReportQuery report = prepareReport(filter, user);

        PurchaseRegisterReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        long totalRecords = summary.getLineItemCount();

        if (exportFormat == ExportFormat.XLSX && totalRecords > EXCEL_ROW_LIMIT) {
            throw new IllegalArgumentException(
                    "Too many rows for Excel (" + totalRecords + "). Narrow the filters or download CSV.");
        }

        String generatedBy = report.user().getFullName();
        LocalDateTime generatedOn = LocalDateTime.now();
        String pharmacies = pharmacyNames(report.pharmacyIds());
        String warehouses = warehouseNames(report.warehouseIds());

        boolean singleDay = report.fromDate().equals(report.toDate());
        String period = singleDay
                ? report.fromDate().format(DISPLAY_DATE)
                : report.fromDate().format(DISPLAY_DATE) + " to " + report.toDate().format(DISPLAY_DATE);
        // purchase-register_2026-10-08, purchase-register_2026-10-01_to_2026-10-08
        String fileName = "purchase-register_"
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

                    writer.title("Purchase Register Report");
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

                    writer.line("Purchase Invoices", summary.getPurchaseInvoiceCount());
                    writer.line("Line Items", summary.getLineItemCount());
                    writer.line("Total Purchase Amount", summary.getTotalPurchaseAmount());
                    writer.line("Taxable Amount", summary.getTaxableAmount());
                    writer.line("GST Amount", summary.getGstAmount());
                    writer.line("GST Share %", summary.getGstSharePercentage());
                    writer.line("Purchased Quantity", summary.getPurchasedQuantity());
                    writer.line("Free Quantity", summary.getFreeQuantity());
                    writer.blank();

                    writer.header(EXPORT_HEADERS);

                    for (long offset = 0; offset < totalRecords; offset += EXPORT_BATCH_SIZE) {

                        List<PurchaseRegisterReportRowDto> batch =
                                fetchPage(report.sql(), report.params(), filter, EXPORT_BATCH_SIZE, offset);

                        for (PurchaseRegisterReportRowDto row : batch) {
                            writer.row(Arrays.asList(
                                    row.getPurchaseDate(), row.getInvoiceNumber(), row.getSupplierName(),
                                    row.getLocationName(), row.getProductName(), row.getBatchNo(),
                                    row.getExpiryDate() == null ? null : row.getExpiryDate().format(EXPIRY_MONTH),
                                    row.getPurchasedQuantity(), row.getFreeQuantity(), row.getPurchaseRate(),
                                    row.getGrossAmount(), row.getDiscount(), row.getTaxableAmount(),
                                    row.getGstPercentage(), row.getGstAmount(), row.getTotalAmount()));
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


    private ReportQuery prepareReport(PurchaseRegisterReportFilter filter, UserDetails user) {

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
     * Builds the report as a CTE; callers wrap it with SELECT ... FROM report.
     *
     *   report - one row per purchase invoice line whose invoice date is in the
     *            period and which was received into an in-scope pharmacy or
     *            warehouse, joined to supplier, location, product and batch and
     *            narrowed by every filter.
     *
     * gross_amount is quantity x rate as entered on the invoice; discount is
     * the line's discount amount; gst is worked out on gross - discount, and
     * net_amount is the resulting line total.
     */
    private String buildReportSql(
            PurchaseRegisterReportFilter filter,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        params.put("fromTs", Timestamp.valueOf(fromDate.atStartOfDay()));
        params.put("toTs", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));

        // An empty list in IN (...) is invalid SQL, so each location kind is
        // only included when it has ids.
        List<String> locationScope = new ArrayList<>();

        if (!pharmacyIds.isEmpty()) {
            params.put("pharmacyIds", pharmacyIds);
            locationScope.add("pu.pharmacy_id IN (:pharmacyIds)");
        }

        if (!warehouseIds.isEmpty()) {
            params.put("warehouseIds", warehouseIds);
            locationScope.add("pu.warehouse_id IN (:warehouseIds)");
        }

        // Each fragment starts with a space so it never glues onto the
        // previous one (a text block would drop that leading space).
        StringBuilder filters = new StringBuilder();

        if (!isEmpty(filter.getSupplierIds())) {
            filters.append(" AND pu.supplier_id IN (:supplierIds)");
            params.put("supplierIds", filter.getSupplierIds());
        }

        if (hasText(filter.getInvoiceNumber())) {
            filters.append(" AND pu.invoice_no ILIKE :invoiceNumber");
            params.put("invoiceNumber", "%" + escapeLike(filter.getInvoiceNumber().trim()) + "%");
        }

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

        YearMonth expiryFrom = parseExpiry(filter.getExpiryFrom(), "expiryFrom");
        YearMonth expiryTo = parseExpiry(filter.getExpiryTo(), "expiryTo");

        if (expiryFrom != null && expiryTo != null && expiryTo.isBefore(expiryFrom)) {
            throw new IllegalArgumentException("expiryTo cannot be before expiryFrom.");
        }

        if (expiryFrom != null) {
            filters.append(" AND b.expiry_date >= :expiryFrom");
            params.put("expiryFrom", Date.valueOf(expiryFrom.atDay(1)));
        }

        if (expiryTo != null) {
            filters.append(" AND b.expiry_date < :expiryTo");
            params.put("expiryTo", Date.valueOf(expiryTo.plusMonths(1).atDay(1)));
        }

        if (!isEmpty(filter.getGstPercentages())) {
            filters.append(" AND ").append(GST_RATE_SQL).append(" IN (:gstPercentages)");
            params.put("gstPercentages", filter.getGstPercentages());
        }

        return """
                WITH report AS (
                    SELECT pd.purchase_details_id AS row_id,
                           pu.purchase_id,
                           COALESCE(pu.invoice_date, pu.created_at) AS purchase_date,
                           pu.invoice_no,
                           sm.supplier_name,
                           COALESCE(wh.warehouse_name, ph.pharmacy_name) AS location_name,
                           p.product_name,
                           b.batch_number,
                           b.expiry_date,
                           COALESCE(pd.purchase_quantity, 0) AS purchased_quantity,
                           COALESCE(pd.free_quantity, 0) AS free_quantity,
                           ROUND(COALESCE(pd.gross_amount, 0)
                                 / NULLIF(pd.purchase_quantity, 0), 2) AS purchase_rate,
                           COALESCE(pd.gross_amount, 0) AS gross_amount,
                           COALESCE(pd.discount, 0) AS discount,
                           COALESCE(pd.gross_amount, 0) - COALESCE(pd.discount, 0) AS taxable_amount,
                           pd.gst_percentage,
                           %2$s AS gst_rate,
                           COALESCE(pd.gst, 0) AS gst_amount,
                           COALESCE(pd.net_amount,
                                    COALESCE(pd.gross_amount, 0) - COALESCE(pd.discount, 0)
                                        + COALESCE(pd.gst, 0)) AS total_amount
                    FROM pharma_purchase_details pd
                    JOIN pharma_purchase pu ON pu.purchase_id = pd.purchase_id
                    JOIN pharma_product_details p ON p.product_id = pd.product_id
                    LEFT JOIN pharma_batch_details b ON b.batch_id = pd.batch_id
                    LEFT JOIN pharma_supplier_master sm ON sm.supplier_id = pu.supplier_id
                    LEFT JOIN pharma_pharmacy_details ph ON ph.pharmacy_id = pu.pharmacy_id
                    LEFT JOIN pharma_warehouse wh ON wh.warehouse_id = pu.warehouse_id
                    WHERE (%1$s)
                      AND COALESCE(pu.invoice_date, pu.created_at) >= :fromTs
                      AND COALESCE(pu.invoice_date, pu.created_at) <  :toTs
                      %3$s
                )
                """.formatted(
                String.join(" OR ", locationScope),
                GST_RATE_SQL,
                filters);
    }


    // "MM/yyyy" or "yyyy-MM"; blank = open end.
    private static YearMonth parseExpiry(String value, String param) {

        if (!hasText(value)) {
            return null;
        }

        for (DateTimeFormatter format : EXPIRY_FORMATS) {
            try {
                return YearMonth.parse(value.trim(), format);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }

        throw new IllegalArgumentException("Invalid " + param + ": " + value + ". Expected MM/yyyy.");
    }


    private PurchaseRegisterReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COUNT(DISTINCT purchase_id),
                       COUNT(*),
                       COALESCE(SUM(total_amount), 0),
                       COALESCE(SUM(taxable_amount), 0),
                       COALESCE(SUM(gst_amount), 0),
                       COALESCE(SUM(purchased_quantity), 0),
                       COALESCE(SUM(free_quantity), 0)
                FROM report
                """, params).getSingleResult();

        BigDecimal totalAmount = toAmount(row[2]);
        BigDecimal gstAmount = toAmount(row[4]);

        PurchaseRegisterReportSummaryDto summary = new PurchaseRegisterReportSummaryDto();
        summary.setPurchaseInvoiceCount(toLong(row[0]));
        summary.setLineItemCount(toLong(row[1]));
        summary.setTotalPurchaseAmount(totalAmount);
        summary.setTaxableAmount(toAmount(row[3]));
        summary.setGstAmount(gstAmount);
        summary.setGstSharePercentage(totalAmount.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : gstAmount.multiply(BigDecimal.valueOf(100)).divide(totalAmount, 2, RoundingMode.HALF_UP));
        summary.setPurchasedQuantity(toLong(row[5]));
        summary.setFreeQuantity(toLong(row[6]));

        return summary;
    }


    @SuppressWarnings("unchecked")
    private List<PurchaseRegisterReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            PurchaseRegisterReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "purchaseDate", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // purchase_id and row_id as tie-breakers keep an invoice's lines
        // together and page boundaries stable.
        Query query = nativeQuery(reportSql + """
                SELECT purchase_date, invoice_no, supplier_name, location_name,
                       product_name, batch_number, expiry_date,
                       purchased_quantity, free_quantity, purchase_rate,
                       gross_amount, discount, taxable_amount,
                       gst_percentage, gst_amount, total_amount
                FROM report
                ORDER BY %s %s NULLS LAST, purchase_id, row_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<PurchaseRegisterReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            PurchaseRegisterReportRowDto dto = new PurchaseRegisterReportRowDto();
            dto.setPurchaseDate(toLocalDate(r[0]));
            dto.setInvoiceNumber((String) r[1]);
            dto.setSupplierName((String) r[2]);
            dto.setLocationName((String) r[3]);
            dto.setProductName((String) r[4]);
            dto.setBatchNo((String) r[5]);
            dto.setExpiryDate(toLocalDate(r[6]));
            dto.setPurchasedQuantity(toLong(r[7]));
            dto.setFreeQuantity(toLong(r[8]));
            dto.setPurchaseRate(r[9] == null ? null : toAmount(r[9]));
            dto.setGrossAmount(toAmount(r[10]));
            dto.setDiscount(toAmount(r[11]));
            dto.setTaxableAmount(toAmount(r[12]));
            dto.setGstPercentage((String) r[13]);
            dto.setGstAmount(toAmount(r[14]));
            dto.setTotalAmount(toAmount(r[15]));
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


    // purchase_date comes back as a timestamp, expiry_date as a date.
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


    private static BigDecimal toAmount(Object value) {

        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal amount = value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
        return amount.setScale(2, RoundingMode.HALF_UP);
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
