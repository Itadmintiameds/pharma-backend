package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.SupplierPaymentModeSummaryDto;
import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportPageDto;
import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportResponse;
import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportRowDto;
import tiameds.pharmabackend.dto.auditReports.SupplierPaymentReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.auditReports.SupplierPaymentReportService;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierPaymentReportServiceImpl implements SupplierPaymentReportService {

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
            "Payment Date", "Supplier Payment No", "Pharmacy / Warehouse", "Supplier",
            "Purchase Invoice No", "GRN No", "Invoice Amount", "Payment Amount", "Payment Mode",
            "Reference / Transaction No", "Outstanding Before", "Outstanding After");

    private static final List<String> MODE_SUMMARY_HEADERS = List.of(
            "Payment Mode", "Transactions", "Total Amount");

    // Supplier payments are only made against credit purchases.
    private static final String PAYMENT_TYPE_CREDIT = "CREDIT";

    // Payment Mode dropdown value -> label shown in the report, in the order
    // of the Payment Mode-wise Summary table. payment_mode is free text, so
    // the SQL folds stored values onto these labels (see MODE_LABEL_SQL).
    private static final Map<String, String> PAYMENT_MODES;

    static {
        Map<String, String> modes = new LinkedHashMap<>();
        modes.put("CASH", "Cash");
        modes.put("UPI", "UPI");
        modes.put("BANK_TRANSFER", "Bank Transfer");
        modes.put("CHEQUE", "Cheque");
        modes.put("OTHER", "Other");
        PAYMENT_MODES = Collections.unmodifiableMap(modes);
    }

    // Normalizes "Bank Transfer" / "bank-transfer" / "NEFT" etc. to a label.
    private static final String MODE_LABEL_SQL = """
            CASE UPPER(REPLACE(REPLACE(TRIM(COALESCE(sp.payment_mode, '')), ' ', '_'), '-', '_'))
                WHEN 'CASH' THEN 'Cash'
                WHEN 'UPI' THEN 'UPI'
                WHEN 'BANK_TRANSFER' THEN 'Bank Transfer'
                WHEN 'BANK' THEN 'Bank Transfer'
                WHEN 'NEFT' THEN 'Bank Transfer'
                WHEN 'RTGS' THEN 'Bank Transfer'
                WHEN 'IMPS' THEN 'Bank Transfer'
                WHEN 'CHEQUE' THEN 'Cheque'
                WHEN 'CHECK' THEN 'Cheque'
                ELSE 'Other'
            END""";

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("paymentDate", "payment_date"),
            Map.entry("paymentNumber", "payment_no"),
            Map.entry("supplierName", "supplier_name"),
            Map.entry("invoiceNumber", "invoice_no"),
            Map.entry("grnNumber", "grn_no"),
            Map.entry("invoiceAmount", "invoice_amount"),
            Map.entry("paymentAmount", "payment_amount"),
            Map.entry("paymentMode", "payment_mode"),
            Map.entry("outstandingBefore", "outstanding_before"),
            Map.entry("outstandingAfter", "outstanding_after"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public SupplierPaymentReportResponse getSupplierPaymentReport(
            SupplierPaymentReportFilter filter,
            UserDetails user) {

        ReportQuery report = prepareReport(filter, user);

        SupplierPaymentReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        long totalRecords = summary.getPaymentCount();

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

        SupplierPaymentReportPageDto details = new SupplierPaymentReportPageDto();
        details.setData(totalRecords == 0
                ? List.of()
                : fetchPage(report.sql(), report.params(), filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        SupplierPaymentReportResponse response = new SupplierPaymentReportResponse();
        response.setSummary(summary);
        response.setPaymentTransactions(details);
        response.setPaymentModeSummary(fetchModeSummary(report.sql(), report.params()));
        response.setPaymentModeTotalCount(totalRecords);
        response.setPaymentModeTotalAmount(summary.getTotalPaymentAmount());
        response.setFromDate(report.fromDate());
        response.setToDate(report.toDate());
        response.setGeneratedBy(report.user().getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Runs the KPI totals (which carry the row count) and the mode summary
    // now, so bad filters, missing access and the Excel row limit fail before
    // any of the file has been sent.
    @Override
    public ReportExport exportSupplierPaymentReport(
            SupplierPaymentReportFilter filter,
            String format,
            UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        ReportQuery report = prepareReport(filter, user);

        SupplierPaymentReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        long totalRecords = summary.getPaymentCount();

        if (exportFormat == ExportFormat.XLSX && totalRecords > EXCEL_ROW_LIMIT) {
            throw new IllegalArgumentException(
                    "Too many rows for Excel (" + totalRecords + "). Narrow the filters or download CSV.");
        }

        List<SupplierPaymentModeSummaryDto> modeSummary = fetchModeSummary(report.sql(), report.params());
        String generatedBy = report.user().getFullName();
        LocalDateTime generatedOn = LocalDateTime.now();
        String pharmacies = pharmacyNames(report.pharmacyIds());
        String warehouses = warehouseNames(report.warehouseIds());

        boolean singleDay = report.fromDate().equals(report.toDate());
        String period = singleDay
                ? report.fromDate().format(DISPLAY_DATE)
                : report.fromDate().format(DISPLAY_DATE) + " to " + report.toDate().format(DISPLAY_DATE);
        // supplier-payment_2026-10-08, supplier-payment_2026-10-01_to_2026-10-08
        String fileName = "supplier-payment_"
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

                    writer.title("Supplier Payment Report");
                    if (!pharmacies.isEmpty()) {
                        writer.line("Pharmacy", pharmacies);
                    }
                    if (!warehouses.isEmpty()) {
                        writer.line("Warehouse", warehouses);
                    }
                    writer.line("Reporting Period", period);
                    writer.line("Generated By", generatedBy);
                    writer.line("Generated On", generatedOn);
                    writer.blank();

                    writer.line("Total Payment Amount", summary.getTotalPaymentAmount());
                    writer.line("Payment Transactions", summary.getPaymentCount());
                    writer.line("Average Payment Amount", summary.getAveragePaymentAmount());
                    writer.line("Suppliers Paid", summary.getSuppliersPaidCount());
                    if (summary.getSuppliersPaid() != null && !summary.getSuppliersPaid().isEmpty()) {
                        writer.line("Suppliers", String.join(", ", summary.getSuppliersPaid()));
                    }
                    writer.line("Invoices Paid Against", summary.getInvoicesPaidAgainstCount());
                    if (summary.getTopInvoiceNumber() != null) {
                        writer.line("Most Instalments", summary.getTopInvoiceNumber()
                                + " (" + summary.getTopInvoiceInstalments() + " instalments)");
                    }
                    writer.blank();

                    // Payment Mode-wise Summary, with its Total row.
                    writer.header(MODE_SUMMARY_HEADERS);
                    for (SupplierPaymentModeSummaryDto mode : modeSummary) {
                        writer.row(Arrays.asList(
                                mode.getPaymentMode(), mode.getTransactionCount(), mode.getTotalAmount()));
                    }
                    writer.row(Arrays.asList("Total", totalRecords, summary.getTotalPaymentAmount()));
                    writer.blank();

                    writer.header(EXPORT_HEADERS);

                    for (long offset = 0; offset < totalRecords; offset += EXPORT_BATCH_SIZE) {

                        List<SupplierPaymentReportRowDto> batch =
                                fetchPage(report.sql(), report.params(), filter, EXPORT_BATCH_SIZE, offset);

                        for (SupplierPaymentReportRowDto row : batch) {
                            writer.row(Arrays.asList(
                                    row.getPaymentDate(), row.getPaymentNumber(),
                                    row.getLocationName(), row.getSupplierName(),
                                    row.getInvoiceNumber(), row.getGrnNumber(),
                                    row.getInvoiceAmount(), row.getPaymentAmount(), row.getPaymentMode(),
                                    row.getTransactionNumber(),
                                    row.getOutstandingBefore(), row.getOutstandingAfter()));
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


    private ReportQuery prepareReport(SupplierPaymentReportFilter filter, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate fromDate = filter.getFromDate() != null ? filter.getFromDate() : LocalDate.now();
        LocalDate toDate = filter.getToDate() != null ? filter.getToDate() : fromDate;

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate cannot be before fromDate.");
        }

        if (filter.getMinPaymentAmount() != null && filter.getMaxPaymentAmount() != null
                && filter.getMaxPaymentAmount().compareTo(filter.getMinPaymentAmount()) < 0) {
            throw new IllegalArgumentException("maxPaymentAmount cannot be less than minPaymentAmount.");
        }

        // A multi-pharmacy organization with centralized inventory buys into
        // its warehouses; a single pharmacy, or a multi-pharmacy organization
        // without a warehouse, buys per pharmacy. Only that location kind
        // carries purchases, so only it is reported on.
        List<String> pharmacyIds = List.of();
        List<String> warehouseIds = List.of();

        if (usesCentralWarehouse(persistentUser)) {

            if (!isEmpty(filter.getPharmacyIds())) {
                throw new IllegalArgumentException(
                        "Purchases are managed in the warehouse for this organization; filter by warehouseIds.");
            }

            List<String> accessible = accessibleWarehouseIds(persistentUser);

            if (accessible.isEmpty()) {
                throw new AccessDeniedException("You do not have access to any warehouse.");
            }

            warehouseIds = isEmpty(filter.getWarehouseIds())
                    ? accessible
                    : checkAccess(filter.getWarehouseIds(), accessible, "warehouse");
        } else {

            if (!isEmpty(filter.getWarehouseIds())) {
                throw new IllegalArgumentException(
                        "Purchases are managed per pharmacy for this organization; filter by pharmacyIds.");
            }

            List<String> accessible = accessiblePharmacyIds(persistentUser);

            if (accessible.isEmpty()) {
                throw new AccessDeniedException("You do not have access to any pharmacy.");
            }

            pharmacyIds = isEmpty(filter.getPharmacyIds())
                    ? accessible
                    : checkAccess(filter.getPharmacyIds(), accessible, "pharmacy");
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


    // Flow 3: centralized_inventory is only ever set for a multi-location
    // organization; a missing organization or flag means per-pharmacy.
    private static boolean usesCentralWarehouse(UserDetails user) {
        return user.getOrganization() != null
                && Boolean.TRUE.equals(user.getOrganization().getCentralizedInventory());
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
     *   invoices - credit purchase invoices received into an in-scope pharmacy
     *              or warehouse, narrowed by the invoice-level filters
     *   payments - every payment of those invoices with the amount paid
     *              before it, so Outstanding Before/After hold even when
     *              earlier instalments fall outside the period or filters
     *   report   - payments in the period, narrowed by the payment-level
     *              filters
     *
     * Same rule as the Supplier Outstanding report: invoice amount is the
     * purchase net total.
     */
    private String buildReportSql(
            SupplierPaymentReportFilter filter,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        // Exactly one location kind is in scope (see usesCentralWarehouse).
        String locationScope;

        if (!warehouseIds.isEmpty()) {
            params.put("warehouseIds", warehouseIds);
            locationScope = "pu.warehouse_id IN (:warehouseIds)";
        } else {
            params.put("pharmacyIds", pharmacyIds);
            locationScope = "pu.pharmacy_id IN (:pharmacyIds)";
        }

        params.put("creditType", PAYMENT_TYPE_CREDIT);
        params.put("confirmedStatus", PurchaseReturnStatus.CONFIRMED.name());
        params.put("fromDate", Date.valueOf(fromDate));
        params.put("toDate", Date.valueOf(toDate));

        // Each fragment starts with a space so it never glues onto the
        // previous one (a text block would drop that leading space).
        StringBuilder invoiceFilters = new StringBuilder();

        if (!isEmpty(filter.getSupplierIds())) {
            invoiceFilters.append(" AND pu.supplier_id IN (:supplierIds)");
            params.put("supplierIds", filter.getSupplierIds());
        }

        if (hasText(filter.getInvoiceNumber())) {
            invoiceFilters.append(" AND pu.invoice_no ILIKE :invoiceNumber");
            params.put("invoiceNumber", "%" + escapeLike(filter.getInvoiceNumber().trim()) + "%");
        }

        if (hasText(filter.getGrnNumber())) {
            invoiceFilters.append(" AND pu.grn_no ILIKE :grnNumber");
            params.put("grnNumber", "%" + escapeLike(filter.getGrnNumber().trim()) + "%");
        }

        // Applied after Outstanding Before is worked out, never inside the
        // payments CTE, so excluded instalments still count as paid before.
        StringBuilder paymentFilters = new StringBuilder();

        if (hasText(filter.getPaymentNumber())) {
            paymentFilters.append(" AND payment_no ILIKE :paymentNumber");
            params.put("paymentNumber", "%" + escapeLike(filter.getPaymentNumber().trim()) + "%");
        }

        if (hasText(filter.getReferenceNumber())) {
            paymentFilters.append(" AND transaction_no ILIKE :referenceNumber");
            params.put("referenceNumber", "%" + escapeLike(filter.getReferenceNumber().trim()) + "%");
        }

        List<String> modes = parsePaymentModes(filter.getPaymentModes());

        if (modes != null) {
            paymentFilters.append(" AND payment_mode IN (:paymentModes)");
            params.put("paymentModes", modes);
        }

        if (filter.getMinPaymentAmount() != null) {
            paymentFilters.append(" AND payment_amount >= :minPaymentAmount");
            params.put("minPaymentAmount", filter.getMinPaymentAmount());
        }

        if (filter.getMaxPaymentAmount() != null) {
            paymentFilters.append(" AND payment_amount <= :maxPaymentAmount");
            params.put("maxPaymentAmount", filter.getMaxPaymentAmount());
        }

        return """
                WITH invoices AS (
                    SELECT pu.purchase_id,
                           pu.supplier_id,
                           COALESCE(wh.warehouse_name, ph.pharmacy_name) AS location_name,
                           sm.supplier_name,
                           pu.invoice_no,
                           pu.grn_no,
                           COALESCE(pu.total_net_amount, 0) AS invoice_amount
                    FROM pharma_purchase pu
                    LEFT JOIN pharma_supplier_master sm ON sm.supplier_id = pu.supplier_id
                    LEFT JOIN pharma_pharmacy_details ph ON ph.pharmacy_id = pu.pharmacy_id
                    LEFT JOIN pharma_warehouse wh ON wh.warehouse_id = pu.warehouse_id
                    WHERE %3$s
                      AND UPPER(pu.payment_type) = :creditType
                      %1$s
                ),
                payments AS (
                    SELECT sp.supplier_payment_id,
                           sp.purchase_id,
                           sp.supplier_payment_no AS payment_no,
                           sp.supplier_payment_date AS payment_date,
                           %4$s AS payment_mode,
                           CAST(COALESCE(sp.payment_amount, 0) AS NUMERIC) AS payment_amount,
                           NULLIF(TRIM(sp.transaction_no), '') AS transaction_no,
                           COALESCE(CAST(SUM(sp.payment_amount) OVER (
                               PARTITION BY sp.purchase_id
                               ORDER BY sp.supplier_payment_date, sp.supplier_payment_id
                               ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING) AS NUMERIC), 0) AS paid_before
                    FROM pharma_supplier_payment sp
                    WHERE sp.purchase_id IN (SELECT purchase_id FROM invoices)
                ),
                report AS (
                    SELECT * FROM (
                        SELECT y.*,
                               GREATEST(y.outstanding_before - y.payment_amount, 0) AS outstanding_after
                        FROM (
                            SELECT pm.*,
                                   i.supplier_id,
                                   i.location_name,
                                   i.supplier_name,
                                   i.invoice_no,
                                   i.grn_no,
                                   i.invoice_amount,
                                   GREATEST(i.invoice_amount - pm.paid_before - COALESCE((
                                       SELECT SUM(COALESCE(pr.total_net_amount, 0))
                                       FROM pharma_purchase_return pr
                                       WHERE pr.purchase_id = pm.purchase_id
                                         AND pr.status = :confirmedStatus
                                         AND CAST(COALESCE(pr.purchase_return_date, pr.created_at) AS DATE)
                                             <= pm.payment_date), 0), 0) AS outstanding_before
                            FROM payments pm
                            JOIN invoices i ON i.purchase_id = pm.purchase_id
                            WHERE pm.payment_date BETWEEN :fromDate AND :toDate
                        ) y
                    ) z
                    WHERE 1 = 1 %2$s
                )
                """.formatted(invoiceFilters, paymentFilters, locationScope, MODE_LABEL_SQL);
    }


    // Dropdown keys -> report labels; null = no restriction (empty or ALL).
    private static List<String> parsePaymentModes(List<String> values) {

        if (isEmpty(values)) {
            return null;
        }

        Set<String> labels = new LinkedHashSet<>();

        for (String value : values) {

            if (!hasText(value)) {
                continue;
            }

            String key = normalizeKey(value);

            if ("ALL".equals(key)) {
                return null;
            }

            String label = PAYMENT_MODES.get(key);

            if (label == null) {
                throw new IllegalArgumentException("Invalid paymentModes: " + value
                        + ". Allowed: ALL, " + String.join(", ", PAYMENT_MODES.keySet()));
            }

            labels.add(label);
        }

        return labels.isEmpty() ? null : List.copyOf(labels);
    }


    @SuppressWarnings("unchecked")
    private SupplierPaymentReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COALESCE(SUM(payment_amount), 0),
                       COUNT(*),
                       COUNT(DISTINCT supplier_id),
                       COUNT(DISTINCT purchase_id)
                FROM report
                """, params).getSingleResult();

        BigDecimal total = toAmount(row[0]);
        long count = toLong(row[1]);

        SupplierPaymentReportSummaryDto summary = new SupplierPaymentReportSummaryDto();
        summary.setTotalPaymentAmount(total);
        summary.setPaymentCount(count);
        summary.setAveragePaymentAmount(count == 0
                ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
        summary.setSuppliersPaidCount(toLong(row[2]));
        summary.setInvoicesPaidAgainstCount(toLong(row[3]));

        if (count == 0) {
            summary.setSuppliersPaid(List.of());
            return summary;
        }

        List<Object> names = nativeQuery(reportSql + """
                SELECT DISTINCT supplier_name FROM report
                WHERE supplier_name IS NOT NULL
                ORDER BY supplier_name
                """, params).getResultList();

        summary.setSuppliersPaid(names.stream().map(String::valueOf).toList());

        // Invoice paid in the most instalments; earliest first payment breaks ties.
        List<Object[]> top = nativeQuery(reportSql + """
                SELECT invoice_no, COUNT(*)
                FROM report
                GROUP BY purchase_id, invoice_no
                ORDER BY COUNT(*) DESC, MIN(payment_date), purchase_id
                LIMIT 1
                """, params).getResultList();

        if (!top.isEmpty()) {
            summary.setTopInvoiceNumber((String) top.get(0)[0]);
            summary.setTopInvoiceInstalments(toLong(top.get(0)[1]));
        }

        return summary;
    }


    // Every mode in table order, 0 where it had no payments.
    @SuppressWarnings("unchecked")
    private List<SupplierPaymentModeSummaryDto> fetchModeSummary(String reportSql, Map<String, Object> params) {

        List<Object[]> results = nativeQuery(reportSql + """
                SELECT payment_mode, COUNT(*), COALESCE(SUM(payment_amount), 0)
                FROM report
                GROUP BY payment_mode
                """, params).getResultList();

        Map<String, Object[]> byMode = new HashMap<>();

        for (Object[] r : results) {
            byMode.put((String) r[0], r);
        }

        List<SupplierPaymentModeSummaryDto> rows = new ArrayList<>(PAYMENT_MODES.size());

        for (String label : PAYMENT_MODES.values()) {
            Object[] r = byMode.get(label);
            SupplierPaymentModeSummaryDto dto = new SupplierPaymentModeSummaryDto();
            dto.setPaymentMode(label);
            dto.setTransactionCount(r == null ? 0L : toLong(r[1]));
            dto.setTotalAmount(r == null ? BigDecimal.ZERO.setScale(2) : toAmount(r[2]));
            rows.add(dto);
        }

        return rows;
    }


    @SuppressWarnings("unchecked")
    private List<SupplierPaymentReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            SupplierPaymentReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "paymentDate", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // supplier_payment_id as tie-breaker keeps page boundaries stable.
        Query query = nativeQuery(reportSql + """
                SELECT supplier_payment_id, payment_date, payment_no,
                       purchase_id, location_name, supplier_name,
                       invoice_no, grn_no, invoice_amount,
                       payment_amount, payment_mode, transaction_no,
                       outstanding_before, outstanding_after
                FROM report
                ORDER BY %s %s NULLS LAST, supplier_payment_id %s
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<SupplierPaymentReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            SupplierPaymentReportRowDto dto = new SupplierPaymentReportRowDto();
            dto.setSupplierPaymentId(toLong(r[0]));
            dto.setPaymentDate(toLocalDate(r[1]));
            dto.setPaymentNumber((String) r[2]);
            dto.setPurchaseId(toLong(r[3]));
            dto.setLocationName((String) r[4]);
            dto.setSupplierName((String) r[5]);
            dto.setInvoiceNumber((String) r[6]);
            dto.setGrnNumber((String) r[7]);
            dto.setInvoiceAmount(toAmount(r[8]));
            dto.setPaymentAmount(toAmount(r[9]));
            dto.setPaymentMode((String) r[10]);
            dto.setTransactionNumber((String) r[11]);
            dto.setOutstandingBefore(toAmount(r[12]));
            dto.setOutstandingAfter(toAmount(r[13]));
            rows.add(dto);
        }

        return rows;
    }


    private Query nativeQuery(String sql, Map<String, Object> params) {

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }


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


    // "Bank Transfer" / "bank-transfer" -> BANK_TRANSFER.
    private static String normalizeKey(String value) {
        return value.trim().toUpperCase().replace(' ', '_').replace('-', '_');
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
