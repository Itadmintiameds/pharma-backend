package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportPageDto;
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportResponse;
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportReturnSummaryDto;
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportReturnSummaryRowDto;
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportRowDto;
import tiameds.pharmabackend.dto.auditReports.SupplierPurchaseReturnReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.auditReports.SupplierPurchaseReturnReportService;
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
public class SupplierPurchaseReturnReportServiceImpl implements SupplierPurchaseReturnReportService {

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
            "Return Date", "Purchase Return No", "Purchase Invoice No", "Supplier", "Pharmacy / Warehouse",
            "Product Name", "Batch No", "Expiry", "Return Reason",
            "Return Quantity", "Free Return Quantity", "Return Rate",
            "Gross Amount", "Discount", "Taxable Amount", "GST %", "GST Amount", "Total Amount", "Status");

    // Status column and title line of the export, as the screen words them.
    private static final Map<String, String> STATUS_LABELS = Map.of(
            "DRAFT", "Draft",
            "CONFIRMED", "Confirmed",
            "CANCELLED", "Cancelled");

    // Expiry Month/Year: the UI format first, ISO as a fallback.
    private static final List<DateTimeFormatter> EXPIRY_FORMATS = List.of(
            DateTimeFormatter.ofPattern("MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM"));

    // Numeric GST rate of a row: gst_percentage is text ("12%", "Exempted"),
    // so it is reduced to its digits; Exempted counts as 0.
    private static final String GST_RATE_SQL =
            "COALESCE(CAST(NULLIF(regexp_replace(gst_percentage, '[^0-9.]', '', 'g'), '') AS NUMERIC), 0)";

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("returnDate", "return_date"),
            Map.entry("purchaseReturnNo", "return_no"),
            Map.entry("purchaseInvoiceNo", "invoice_no"),
            Map.entry("supplierName", "supplier_name"),
            Map.entry("locationName", "location_name"),
            Map.entry("productName", "product_name"),
            Map.entry("batchNo", "batch_number"),
            Map.entry("expiryDate", "expiry_date"),
            Map.entry("returnReason", "return_reason"),
            Map.entry("returnQuantity", "return_quantity"),
            Map.entry("freeReturnQuantity", "free_return_quantity"),
            Map.entry("returnRate", "return_rate"),
            Map.entry("grossAmount", "gross_amount"),
            Map.entry("discount", "discount"),
            Map.entry("taxableAmount", "taxable_amount"),
            Map.entry("gstPercentage", "gst_rate"),
            Map.entry("gstAmount", "gst_amount"),
            Map.entry("totalAmount", "total_amount"),
            Map.entry("status", "status"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public SupplierPurchaseReturnReportResponse getSupplierPurchaseReturnReport(
            SupplierPurchaseReturnReportFilter filter,
            UserDetails user) {

        ReportQuery report = prepareReport(filter, user);

        SupplierPurchaseReturnReportSummaryDto summary = fetchSummary(report.sql(), report.params());
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

        SupplierPurchaseReturnReportPageDto details = new SupplierPurchaseReturnReportPageDto();
        details.setData(totalRecords == 0
                ? List.of()
                : fetchPage(report.sql(), report.params(), filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        SupplierPurchaseReturnReportResponse response = new SupplierPurchaseReturnReportResponse();
        response.setSummary(summary);
        response.setPurchaseReturnDetails(details);
        response.setPurchaseReturnSummary(buildReturnSummary(summary, filter.getStatuses()));
        response.setGeneratedBy(report.user().getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Runs the count and the KPI totals now, so bad filters, missing access
    // and the Excel row limit fail before any of the file has been sent.
    @Override
    public ReportExport exportSupplierPurchaseReturnReport(
            SupplierPurchaseReturnReportFilter filter,
            String format,
            UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        ReportQuery report = prepareReport(filter, user);

        long totalRecords = fetchTotalRecords(report.sql(), report.params());

        if (exportFormat == ExportFormat.XLSX && totalRecords > EXCEL_ROW_LIMIT) {
            throw new IllegalArgumentException(
                    "Too many rows for Excel (" + totalRecords + "). Narrow the filters or download CSV.");
        }

        SupplierPurchaseReturnReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        SupplierPurchaseReturnReportReturnSummaryDto returnSummary =
                buildReturnSummary(summary, filter.getStatuses());
        String generatedBy = report.user().getFullName();
        LocalDateTime generatedOn = LocalDateTime.now();
        String pharmacies = pharmacyNames(report.pharmacyIds());
        String warehouses = warehouseNames(report.warehouseIds());
        String statuses = statusesLabel(filter.getStatuses());

        boolean singleDay = report.fromDate().equals(report.toDate());
        String period = singleDay
                ? report.fromDate().format(DISPLAY_DATE)
                : report.fromDate().format(DISPLAY_DATE) + " to " + report.toDate().format(DISPLAY_DATE);
        // supplier-purchase-return_2026-10-08, supplier-purchase-return_2026-10-01_to_2026-10-08
        String fileName = "supplier-purchase-return_"
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

                    writer.title("Supplier Purchase Return Report");
                    if (!pharmacies.isEmpty()) {
                        writer.line("Pharmacy", pharmacies);
                    }
                    if (!warehouses.isEmpty()) {
                        writer.line("Warehouse", warehouses);
                    }
                    writer.line("Period", period);
                    writer.line("Status", statuses);
                    writer.line("Generated By", generatedBy);
                    writer.line("Generated On", generatedOn);
                    writer.blank();

                    writer.line("Purchase Returns", summary.getPurchaseReturnCount());
                    writer.line("Purchase Invoices", summary.getPurchaseInvoiceCount());
                    writer.line("Total Return Amount", summary.getTotalReturnAmount());
                    writer.line("Taxable Amount", summary.getTaxableAmount());
                    writer.line("GST Reversal Amount", summary.getGstReversalAmount());
                    writer.line("GST Share %", summary.getGstSharePercentage());
                    writer.line("Total Physical Return Quantity", summary.getTotalPhysicalReturnQuantity());
                    writer.line("Free Return Quantity", summary.getFreeReturnQuantity());
                    writer.line("Return Purchase Quantity", summary.getReturnQuantity());
                    writer.line("Gross Return Amount", summary.getGrossAmount());
                    writer.line("Discount Amount", summary.getDiscountAmount());
                    writer.blank();

                    writer.header(EXPORT_HEADERS);

                    for (long offset = 0; offset < totalRecords; offset += EXPORT_BATCH_SIZE) {

                        List<SupplierPurchaseReturnReportRowDto> batch =
                                fetchPage(report.sql(), report.params(), filter, EXPORT_BATCH_SIZE, offset);

                        for (SupplierPurchaseReturnReportRowDto row : batch) {
                            writer.row(Arrays.asList(
                                    row.getReturnDate(), row.getPurchaseReturnNo(), row.getPurchaseInvoiceNo(),
                                    row.getSupplierName(), row.getLocationName(),
                                    row.getProductName(), row.getBatchNo(),
                                    row.getExpiryDate() == null ? null : row.getExpiryDate().format(EXPIRY_MONTH),
                                    row.getReturnReason(),
                                    row.getReturnQuantity(), row.getFreeReturnQuantity(), row.getReturnRate(),
                                    row.getGrossAmount(), row.getDiscount(), row.getTaxableAmount(),
                                    row.getGstPercentage(), row.getGstAmount(), row.getTotalAmount(),
                                    STATUS_LABELS.getOrDefault(row.getStatus(), row.getStatus())));
                        }

                        if (batch.isEmpty()) {
                            break;
                        }
                    }

                    writer.blank();
                    writer.line("Supplier Purchase Return Summary", null);
                    writer.header(List.of("Metric", "Value"));
                    for (SupplierPurchaseReturnReportReturnSummaryRowDto row : returnSummary.getRows()) {
                        writer.row(Arrays.asList(row.getLabel(), row.getValue()));
                    }
                    writer.row(Arrays.asList("Total Purchase Return Amount",
                            returnSummary.getTotalPurchaseReturnAmount()));

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


    private ReportQuery prepareReport(SupplierPurchaseReturnReportFilter filter, UserDetails user) {

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


    // The statuses the export covers, as parseStatuses reads the filter: none
    // sent = Confirmed, ALL = every status.
    private static String statusesLabel(List<String> requested) {

        if (isEmpty(requested)) {
            return STATUS_LABELS.get("CONFIRMED");
        }

        if (requested.stream().anyMatch(s -> "ALL".equalsIgnoreCase(s.trim()))) {
            return "All";
        }

        return String.join(", ", requested.stream()
                .map(s -> STATUS_LABELS.getOrDefault(s.trim().toUpperCase(), s.trim()))
                .distinct()
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
     *   base   - one row per current (latest-revision) purchase return line whose
     *            return date is in the period and which was returned from an
     *            in-scope pharmacy or warehouse; deleted drafts are left out.
     *            Supplier and invoice number come from the original purchase;
     *            the GST slab from the matching invoice line (same product and
     *            batch), falling back to the product master.
     *   report - base narrowed by GST rate, which needs the resolved slab.
     *
     * discount is the line's server-derived share of the invoice discount;
     * total is worked out as gross - discount + gst so it always agrees with
     * the taxable and GST columns next to it.
     */
    private String buildReportSql(
            SupplierPurchaseReturnReportFilter filter,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        params.put("fromTs", Timestamp.valueOf(fromDate.atStartOfDay()));
        params.put("toTs", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        params.put("statuses", parseStatuses(filter.getStatuses()));

        // An empty list in IN (...) is invalid SQL, so each location kind is
        // only included when it has ids.
        List<String> locationScope = new ArrayList<>();

        if (!pharmacyIds.isEmpty()) {
            params.put("pharmacyIds", pharmacyIds);
            locationScope.add("(x.warehouse_id IS NULL AND x.pharmacy_id IN (:pharmacyIds))");
        }

        if (!warehouseIds.isEmpty()) {
            params.put("warehouseIds", warehouseIds);
            locationScope.add("x.warehouse_id IN (:warehouseIds)");
        }

        // Each fragment starts with a space so it never glues onto the
        // previous one (a text block would drop that leading space).
        StringBuilder filters = new StringBuilder();

        if (!isEmpty(filter.getSupplierIds())) {
            filters.append(" AND pu.supplier_id IN (:supplierIds)");
            params.put("supplierIds", filter.getSupplierIds());
        }

        if (hasText(filter.getPurchaseReturnNo())) {
            filters.append(" AND x.return_no ILIKE :purchaseReturnNo");
            params.put("purchaseReturnNo", "%" + escapeLike(filter.getPurchaseReturnNo().trim()) + "%");
        }

        if (hasText(filter.getPurchaseInvoiceNo())) {
            filters.append(" AND pu.invoice_no ILIKE :purchaseInvoiceNo");
            params.put("purchaseInvoiceNo", "%" + escapeLike(filter.getPurchaseInvoiceNo().trim()) + "%");
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

        StringBuilder reportFilters = new StringBuilder();

        if (!isEmpty(filter.getGstPercentages())) {
            reportFilters.append(" AND gst_rate IN (:gstPercentages)");
            params.put("gstPercentages", filter.getGstPercentages());
        }

        return """
                WITH base AS (
                    SELECT d.purchase_return_detail_id AS row_id,
                           x.purchase_return_id,
                           x.purchase_id,
                           COALESCE(x.purchase_return_date, x.created_at) AS return_date,
                           x.return_no,
                           x.status,
                           pu.invoice_no,
                           sm.supplier_name,
                           COALESCE(wh.warehouse_name, ph.pharmacy_name) AS location_name,
                           p.product_name,
                           b.batch_number,
                           b.expiry_date,
                           d.return_reason,
                           COALESCE(d.purchase_return_quantity, 0) AS return_quantity,
                           COALESCE(d.free_return_quantity, 0) AS free_return_quantity,
                           ROUND(COALESCE(d.gross_amount, 0)
                                 / NULLIF(d.purchase_return_quantity, 0), 2) AS return_rate,
                           COALESCE(d.gross_amount, 0) AS gross_amount,
                           COALESCE(d.discount_amount, 0) AS discount,
                           COALESCE(d.gross_amount, 0) - COALESCE(d.discount_amount, 0) AS taxable_amount,
                           COALESCE(opl.gst_percentage, p.gst_percentage) AS gst_percentage,
                           COALESCE(d.gst_amount, 0) AS gst_amount,
                           COALESCE(d.gross_amount, 0) - COALESCE(d.discount_amount, 0)
                               + COALESCE(d.gst_amount, 0) AS total_amount
                    FROM pharma_purchase_return x
                    JOIN pharma_purchase_return_details d
                      ON d.purchase_return_id = x.purchase_return_id
                    JOIN pharma_product_details p ON p.product_id = d.product_id
                    LEFT JOIN pharma_purchase pu ON pu.purchase_id = x.purchase_id
                    LEFT JOIN pharma_supplier_master sm ON sm.supplier_id = pu.supplier_id
                    LEFT JOIN pharma_batch_details b ON b.batch_id = d.batch_id
                    LEFT JOIN pharma_pharmacy_details ph ON ph.pharmacy_id = x.pharmacy_id
                    LEFT JOIN pharma_warehouse wh ON wh.warehouse_id = x.warehouse_id
                    LEFT JOIN LATERAL (
                        SELECT pd.gst_percentage
                        FROM pharma_purchase_details pd
                        WHERE pd.purchase_id = x.purchase_id
                          AND pd.product_id = d.product_id
                          AND pd.batch_id IS NOT DISTINCT FROM d.batch_id
                        ORDER BY pd.purchase_details_id
                        LIMIT 1
                    ) opl ON TRUE
                    WHERE (%1$s)
                      AND x.status IN (:statuses)
                      AND COALESCE(x.is_draft_deleted, FALSE) = FALSE
                      AND COALESCE(d.is_active, TRUE) = TRUE
                      AND COALESCE(x.purchase_return_date, x.created_at) >= :fromTs
                      AND COALESCE(x.purchase_return_date, x.created_at) <  :toTs
                      %2$s
                ),
                report AS (
                    SELECT *
                    FROM (SELECT base.*, %3$s AS gst_rate FROM base) r
                    WHERE 1 = 1 %4$s
                )
                """.formatted(
                String.join(" OR ", locationScope),
                filters,
                GST_RATE_SQL,
                reportFilters);
    }


    // DRAFT / CONFIRMED / CANCELLED (any case). Not sent = CONFIRMED, the
    // screen's default; ALL = every status.
    private static List<String> parseStatuses(List<String> requested) {

        if (isEmpty(requested)) {
            return List.of(PurchaseReturnStatus.CONFIRMED.name());
        }

        Set<String> statuses = new LinkedHashSet<>();

        for (String value : requested) {
            if (!hasText(value)) {
                continue;
            }

            String code = value.trim().toUpperCase();

            if ("ALL".equals(code)) {
                return Arrays.stream(PurchaseReturnStatus.values()).map(Enum::name).toList();
            }

            try {
                statuses.add(PurchaseReturnStatus.valueOf(code).name());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + value
                        + ". Allowed: ALL, " + Arrays.toString(PurchaseReturnStatus.values()));
            }
        }

        return statuses.isEmpty()
                ? List.of(PurchaseReturnStatus.CONFIRMED.name())
                : new ArrayList<>(statuses);
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


    private SupplierPurchaseReturnReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COUNT(DISTINCT purchase_return_id),
                       COUNT(DISTINCT purchase_id),
                       COALESCE(SUM(total_amount), 0),
                       COALESCE(SUM(taxable_amount), 0),
                       COALESCE(SUM(gst_amount), 0),
                       COALESCE(SUM(return_quantity + free_return_quantity), 0),
                       COALESCE(SUM(free_return_quantity), 0),
                       COALESCE(SUM(return_quantity), 0),
                       COALESCE(SUM(gross_amount), 0),
                       COALESCE(SUM(discount), 0)
                FROM report
                """, params).getSingleResult();

        BigDecimal totalAmount = toAmount(row[2]);
        BigDecimal gstAmount = toAmount(row[4]);

        SupplierPurchaseReturnReportSummaryDto summary = new SupplierPurchaseReturnReportSummaryDto();
        summary.setPurchaseReturnCount(toLong(row[0]));
        summary.setPurchaseInvoiceCount(toLong(row[1]));
        summary.setTotalReturnAmount(totalAmount);
        summary.setTaxableAmount(toAmount(row[3]));
        summary.setGstReversalAmount(gstAmount);
        summary.setGstSharePercentage(totalAmount.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : gstAmount.multiply(BigDecimal.valueOf(100)).divide(totalAmount, 2, RoundingMode.HALF_UP));
        summary.setTotalPhysicalReturnQuantity(toLong(row[5]));
        summary.setFreeReturnQuantity(toLong(row[6]));
        summary.setReturnQuantity(toLong(row[7]));
        summary.setGrossAmount(toAmount(row[8]));
        summary.setDiscountAmount(toAmount(row[9]));

        return summary;
    }


    // Supplier Purchase Return Summary table, in the design's order. Same
    // numbers as the KPI cards and the Total row, so no extra query. The
    // first metric reads "Confirmed Purchase Returns" only when the report is
    // limited to Confirmed (the default), so the label matches what's counted.
    private SupplierPurchaseReturnReportReturnSummaryDto buildReturnSummary(
            SupplierPurchaseReturnReportSummaryDto summary,
            List<String> requestedStatuses) {

        String count = SupplierPurchaseReturnReportReturnSummaryRowDto.COUNT;
        String amount = SupplierPurchaseReturnReportReturnSummaryRowDto.AMOUNT;

        boolean confirmedOnly = parseStatuses(requestedStatuses)
                .equals(List.of(PurchaseReturnStatus.CONFIRMED.name()));

        List<SupplierPurchaseReturnReportReturnSummaryRowDto> rows = List.of(
                new SupplierPurchaseReturnReportReturnSummaryRowDto("PURCHASE_RETURNS",
                        confirmedOnly ? "Confirmed Purchase Returns" : "Purchase Returns",
                        count, BigDecimal.valueOf(summary.getPurchaseReturnCount())),
                new SupplierPurchaseReturnReportReturnSummaryRowDto("RETURN_PURCHASE_QUANTITY",
                        "Return Purchase Quantity", count, BigDecimal.valueOf(summary.getReturnQuantity())),
                new SupplierPurchaseReturnReportReturnSummaryRowDto("RETURN_FREE_QUANTITY",
                        "Return Free Quantity", count, BigDecimal.valueOf(summary.getFreeReturnQuantity())),
                new SupplierPurchaseReturnReportReturnSummaryRowDto("TOTAL_PHYSICAL_RETURN_QUANTITY",
                        "Total Physical Return Quantity", count,
                        BigDecimal.valueOf(summary.getTotalPhysicalReturnQuantity())),
                new SupplierPurchaseReturnReportReturnSummaryRowDto("GROSS_RETURN_AMOUNT",
                        "Gross Return Amount", amount, summary.getGrossAmount()),
                new SupplierPurchaseReturnReportReturnSummaryRowDto("DISCOUNT_AMOUNT",
                        "Discount Amount", amount, summary.getDiscountAmount()),
                new SupplierPurchaseReturnReportReturnSummaryRowDto("TAXABLE_RETURN_AMOUNT",
                        "Taxable Return Amount", amount, summary.getTaxableAmount()),
                new SupplierPurchaseReturnReportReturnSummaryRowDto("GST_REVERSAL_AMOUNT",
                        "GST Reversal Amount", amount, summary.getGstReversalAmount()));

        return new SupplierPurchaseReturnReportReturnSummaryDto(rows, summary.getTotalReturnAmount());
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<SupplierPurchaseReturnReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            SupplierPurchaseReturnReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "returnDate", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // purchase_return_id and row_id as tie-breakers keep a return's lines
        // together and page boundaries stable.
        Query query = nativeQuery(reportSql + """
                SELECT return_date, return_no, invoice_no, supplier_name, location_name,
                       product_name, batch_number, expiry_date, return_reason,
                       return_quantity, free_return_quantity, return_rate,
                       gross_amount, discount, taxable_amount,
                       gst_percentage, gst_amount, total_amount, status
                FROM report
                ORDER BY %s %s NULLS LAST, purchase_return_id, row_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<SupplierPurchaseReturnReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            SupplierPurchaseReturnReportRowDto dto = new SupplierPurchaseReturnReportRowDto();
            dto.setReturnDate(toLocalDate(r[0]));
            dto.setPurchaseReturnNo((String) r[1]);
            dto.setPurchaseInvoiceNo((String) r[2]);
            dto.setSupplierName((String) r[3]);
            dto.setLocationName((String) r[4]);
            dto.setProductName((String) r[5]);
            dto.setBatchNo((String) r[6]);
            dto.setExpiryDate(toLocalDate(r[7]));
            dto.setReturnReason((String) r[8]);
            dto.setReturnQuantity(toLong(r[9]));
            dto.setFreeReturnQuantity(toLong(r[10]));
            dto.setReturnRate(r[11] == null ? null : toAmount(r[11]));
            dto.setGrossAmount(toAmount(r[12]));
            dto.setDiscount(toAmount(r[13]));
            dto.setTaxableAmount(toAmount(r[14]));
            dto.setGstPercentage((String) r[15]);
            dto.setGstAmount(toAmount(r[16]));
            dto.setTotalAmount(toAmount(r[17]));
            dto.setStatus((String) r[18]);
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
