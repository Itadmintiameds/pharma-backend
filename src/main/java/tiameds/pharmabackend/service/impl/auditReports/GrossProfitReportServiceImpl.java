package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportFilter;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportPageDto;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportResponse;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportRowDto;
import tiameds.pharmabackend.dto.auditReports.GrossProfitReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.auditReports.GrossProfitReportService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;
import tiameds.pharmabackend.service.impl.financialReport.export.ExportFormat;
import tiameds.pharmabackend.service.impl.financialReport.export.ReportTableWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrossProfitReportServiceImpl implements GrossProfitReportService {

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
            "Pharmacy", "Product Name", "Batch No",
            "Sold Quantity", "Return Quantity", "Net Quantity",
            "Net Sales Amount", "Net Cost", "Gross Profit", "Margin %");

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // Customer/Patient types offered on the report filter.
    private static final Set<CustomerType> REPORT_CUSTOMER_TYPES = EnumSet.of(
            CustomerType.WALK_IN,
            CustomerType.OP_PATIENT,
            CustomerType.IP_PATIENT,
            CustomerType.DAYCARE);

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("pharmacyName", "pharmacy_name"),
            Map.entry("productName", "product_name"),
            Map.entry("batchNumber", "batch_number"),
            Map.entry("soldQuantity", "sold_quantity"),
            Map.entry("returnQuantity", "return_quantity"),
            Map.entry("netQuantity", "net_quantity"),
            Map.entry("netSalesAmount", "net_sales_amount"),
            Map.entry("netCost", "net_cost"),
            Map.entry("grossProfit", "gross_profit"),
            Map.entry("marginPercentage", "margin_percentage"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public GrossProfitReportResponse getGrossProfitReport(
            GrossProfitReportFilter filter,
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

        GrossProfitReportPageDto details = new GrossProfitReportPageDto();
        details.setData(fetchPage(report.sql(), report.params(), filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        GrossProfitReportResponse response = new GrossProfitReportResponse();
        response.setSummary(fetchSummary(report.sql(), report.params()));
        response.setProductProfitability(details);
        response.setFromDate(report.fromDate());
        response.setToDate(report.toDate());
        response.setGeneratedBy(report.user().getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Runs the count and the KPI totals now, so bad filters, missing access
    // and the Excel row limit fail before any of the file has been sent.
    @Override
    public ReportExport exportGrossProfitReport(
            GrossProfitReportFilter filter,
            String format,
            UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        ReportQuery report = prepareReport(filter, user);

        long totalRecords = fetchTotalRecords(report.sql(), report.params());

        if (exportFormat == ExportFormat.XLSX && totalRecords > EXCEL_ROW_LIMIT) {
            throw new IllegalArgumentException(
                    "Too many rows for Excel (" + totalRecords + "). Narrow the filters or download CSV.");
        }

        GrossProfitReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        String generatedBy = report.user().getFullName();
        LocalDateTime generatedOn = LocalDateTime.now();
        String pharmacies = pharmacyNames(report.pharmacyIds());

        boolean singleDay = report.fromDate().equals(report.toDate());
        String period = singleDay
                ? report.fromDate().format(DISPLAY_DATE)
                : report.fromDate().format(DISPLAY_DATE) + " to " + report.toDate().format(DISPLAY_DATE);
        // gross-profit_2026-10-08, gross-profit_2026-10-01_to_2026-10-08
        String fileName = "gross-profit_"
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

                    writer.title("Gross Profit / Margin Analysis Report");
                    writer.line("Pharmacy", pharmacies);
                    writer.line("Reporting Period", period);
                    writer.line("Generated By", generatedBy);
                    writer.line("Generated On", generatedOn);
                    writer.blank();

                    writer.line("Net Sales Amount", summary.getNetSalesAmount());
                    writer.line("Net Cost of Goods Sold", summary.getNetCostOfGoodsSold());
                    writer.line("Cost % of Net Sales", summary.getCostPercentage());
                    writer.line("Gross Profit", summary.getGrossProfit());
                    writer.line("Gross Margin %", summary.getGrossMargin());
                    writer.line("Sold Quantity", summary.getTotalSoldQuantity());
                    writer.line("Return Quantity", summary.getTotalReturnQuantity());
                    writer.line("Net Quantity", summary.getTotalNetQuantity());
                    writer.line("Products", summary.getTotalProducts());
                    writer.line("Profitable Products", summary.getProfitableProducts());
                    writer.line("Loss-making Products", summary.getLossMakingProducts());
                    writer.blank();

                    writer.header(EXPORT_HEADERS);

                    for (long offset = 0; offset < totalRecords; offset += EXPORT_BATCH_SIZE) {

                        List<GrossProfitReportRowDto> batch =
                                fetchPage(report.sql(), report.params(), filter, EXPORT_BATCH_SIZE, offset);

                        for (GrossProfitReportRowDto row : batch) {
                            writer.row(Arrays.asList(
                                    row.getPharmacyName(), row.getProductName(), row.getBatchNumber(),
                                    row.getSoldQuantity(), row.getReturnQuantity(), row.getNetQuantity(),
                                    row.getNetSalesAmount(), row.getNetCost(), row.getGrossProfit(),
                                    row.getMarginPercentage()));
                        }

                        if (batch.isEmpty()) {
                            break;
                        }
                    }

                    writer.row(Arrays.asList(
                            "Total", null, null,
                            summary.getTotalSoldQuantity(), summary.getTotalReturnQuantity(),
                            summary.getTotalNetQuantity(),
                            summary.getNetSalesAmount(), summary.getNetCostOfGoodsSold(),
                            summary.getGrossProfit(), summary.getGrossMargin()));

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


    private ReportQuery prepareReport(GrossProfitReportFilter filter, UserDetails user) {

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


    // Pharmacies the user may report on: every pharmacy in the organization
    // for a SUPER ADMIN, otherwise the ones mapped to the user. An empty
    // request means all of them; anything outside that set is refused.
    private List<String> resolvePharmacyIds(List<String> requested, UserDetails user) {

        List<String> accessible;

        if (locationContextResolver.isSuperAdmin(user) && user.getOrganization() != null) {
            accessible = pharmacyDetailsRepository
                    .findAllByOrganization_OrganizationIdOrderByPharmacyCity(
                            user.getOrganization().getOrganizationId())
                    .stream()
                    .map(PharmacyDetails::getPharmacyId)
                    .toList();
        } else {
            accessible = pharmacyDetailsRepository.findPharmacyIdsByUserId(user.getUserId());
        }

        if (accessible.isEmpty()) {
            throw new AccessDeniedException("You do not have access to any pharmacy.");
        }

        if (isEmpty(requested)) {
            return accessible;
        }

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
     * Builds the report as a CTE chain; callers wrap it with SELECT ... FROM report.
     *
     *   sales   - billed quantity / taxable amount per product + batch, bills
     *             in the period
     *   returns - returned quantity / taxable amount per product + batch,
     *             Completed returns dated in the period (so a return of an
     *             older bill still counts against the period it happened in)
     *   lines   - every product + batch that was sold or returned, net of
     *             returns, with product / batch filters applied once here
     *   report  - lines with gross profit and margin. Margin is 0 when net
     *             sales are not positive (e.g. a fully returned line left at
     *             -0.01 by the return lines' rounding), where it means nothing.
     *
     * Revenue is the taxable amount (after discount, before GST), since GST
     * is not income. Cost is the net quantity at the batch's purchase price
     * per smallest unit, the unit bill and return quantities are kept in.
     *
     * Bill-level filters (bill no, customer type, sales return no) are applied
     * in both halves; for returns they are read from the original bill.
     */
    private String buildReportSql(
            GrossProfitReportFilter filter,
            List<String> pharmacyIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        params.put("pharmacyIds", pharmacyIds);
        params.put("fromTs", fromDate.atStartOfDay());
        params.put("toTs", toDate.plusDays(1).atStartOfDay());
        params.put("completedStatus", SalesReturnStatus.COMPLETED.getLabel());

        StringBuilder billFilters = new StringBuilder();

        if (!isEmpty(filter.getCustomerTypes())) {
            billFilters.append(" AND b.customer_type IN (:customerTypes)");
            params.put("customerTypes", parseCustomerTypes(filter.getCustomerTypes()));
        }

        appendSearch(billFilters, params, "b.bill_no", "billNo", filter.getBillNo());

        // Sales Return No narrows sales to the bills that return was made
        // against, and returns to the matching returns themselves.
        StringBuilder salesFilters = new StringBuilder();
        StringBuilder returnFilters = new StringBuilder();

        if (hasText(filter.getSalesReturnNo())) {
            salesFilters.append(" ").append("""
                     AND EXISTS (
                         SELECT 1
                         FROM pharma_sales_return x
                         WHERE x.billing_id = b.billing_id
                           AND x.sales_return_status = :completedStatus
                           AND x.sales_return_no ILIKE :salesReturnNo
                     )""");
            returnFilters.append(" AND sr.sales_return_no ILIKE :salesReturnNo");
            params.put("salesReturnNo", "%" + escapeLike(filter.getSalesReturnNo().trim()) + "%");
        }

        StringBuilder lineFilters = new StringBuilder();

        if (!isEmpty(filter.getProductIds())) {
            lineFilters.append(" AND p.product_id IN (:productIds)");
            params.put("productIds", filter.getProductIds());
        }

        appendSearch(lineFilters, params, "p.product_name", "productName", filter.getProductName());
        appendSearch(lineFilters, params, "bt.batch_number", "batchNumber", filter.getBatchNumber());

        return """
                WITH sales AS (
                    SELECT b.pharmacy_id,
                           bd.product_id,
                           bd.batch_id,
                           SUM(bd.bill_quantity) AS qty,
                           SUM(bd.gross_amount)  AS amount
                    FROM pharma_billing_details bd
                    JOIN pharma_billing b ON b.billing_id = bd.billing_id
                    WHERE b.pharmacy_id IN (:pharmacyIds)
                      AND b.created_at >= :fromTs
                      AND b.created_at <  :toTs
                      %1$s
                      %2$s
                    GROUP BY b.pharmacy_id, bd.product_id, bd.batch_id
                ),
                returns AS (
                    SELECT sr.pharmacy_id,
                           srd.product_id,
                           srd.batch_id,
                           SUM(srd.sales_return_quantity) AS qty,
                           SUM(srd.gross_amount)          AS amount
                    FROM pharma_sales_return_details srd
                    JOIN pharma_sales_return sr ON sr.sales_return_id = srd.sales_return_id
                    JOIN pharma_billing b ON b.billing_id = sr.billing_id
                    WHERE sr.pharmacy_id IN (:pharmacyIds)
                      AND sr.sales_return_status = :completedStatus
                      AND COALESCE(sr.sales_return_date, sr.created_at) >= :fromTs
                      AND COALESCE(sr.sales_return_date, sr.created_at) <  :toTs
                      %1$s
                      %3$s
                    GROUP BY sr.pharmacy_id, srd.product_id, srd.batch_id
                ),
                lines AS (
                    SELECT ids.pharmacy_id,
                           ph.pharmacy_name,
                           p.product_id,
                           p.product_name,
                           ids.batch_id,
                           bt.batch_number,
                           COALESCE(s.qty, 0)                            AS sold_quantity,
                           COALESCE(r.qty, 0)                            AS return_quantity,
                           COALESCE(s.qty, 0) - COALESCE(r.qty, 0)       AS net_quantity,
                           COALESCE(s.amount, 0) - COALESCE(r.amount, 0) AS net_sales_amount,
                           (COALESCE(s.qty, 0) - COALESCE(r.qty, 0))
                               * COALESCE(CAST(bt.purchase_price_per_unit AS NUMERIC), 0) AS net_cost
                    FROM (SELECT pharmacy_id, product_id, batch_id FROM sales
                          UNION
                          SELECT pharmacy_id, product_id, batch_id FROM returns) ids
                    JOIN pharma_product_details p ON p.product_id = ids.product_id
                    JOIN pharma_pharmacy_details ph ON ph.pharmacy_id = ids.pharmacy_id
                    LEFT JOIN pharma_batch_details bt ON bt.batch_id = ids.batch_id
                    LEFT JOIN sales s
                           ON s.pharmacy_id = ids.pharmacy_id
                          AND s.product_id = ids.product_id
                          AND s.batch_id IS NOT DISTINCT FROM ids.batch_id
                    LEFT JOIN returns r
                           ON r.pharmacy_id = ids.pharmacy_id
                          AND r.product_id = ids.product_id
                          AND r.batch_id IS NOT DISTINCT FROM ids.batch_id
                    WHERE 1 = 1
                      %4$s
                ),
                report AS (
                    SELECT l.*,
                           l.net_sales_amount - l.net_cost AS gross_profit,
                           CASE WHEN l.net_sales_amount > 0
                                THEN (l.net_sales_amount - l.net_cost) * 100 / l.net_sales_amount
                                ELSE 0
                           END AS margin_percentage
                    FROM lines l
                )
                """.formatted(billFilters, salesFilters, returnFilters, lineFilters);
    }


    private GrossProfitReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COALESCE(SUM(sold_quantity), 0),
                       COALESCE(SUM(return_quantity), 0),
                       COALESCE(SUM(net_quantity), 0),
                       COALESCE(SUM(net_sales_amount), 0),
                       COALESCE(SUM(net_cost), 0),
                       COALESCE(SUM(gross_profit), 0),
                       (SELECT COUNT(DISTINCT product_id) FROM report),
                       (SELECT COUNT(*) FROM (SELECT product_id FROM report
                                              GROUP BY product_id
                                              HAVING SUM(gross_profit) > 0) t),
                       (SELECT COUNT(*) FROM (SELECT product_id FROM report
                                              GROUP BY product_id
                                              HAVING SUM(gross_profit) < 0) t)
                FROM report
                """, params).getSingleResult();

        GrossProfitReportSummaryDto summary = new GrossProfitReportSummaryDto();
        summary.setTotalSoldQuantity(toLong(row[0]));
        summary.setTotalReturnQuantity(toLong(row[1]));
        summary.setTotalNetQuantity(toLong(row[2]));
        summary.setNetSalesAmount(toAmount(row[3]));
        summary.setNetCostOfGoodsSold(toAmount(row[4]));
        summary.setGrossProfit(toAmount(row[5]));
        summary.setTotalProducts(toLong(row[6]));
        summary.setProfitableProducts(toLong(row[7]));
        summary.setLossMakingProducts(toLong(row[8]));
        summary.setCostPercentage(
                percentage(summary.getNetCostOfGoodsSold(), summary.getNetSalesAmount()));
        summary.setGrossMargin(
                percentage(summary.getGrossProfit(), summary.getNetSalesAmount()));

        return summary;
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<GrossProfitReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            GrossProfitReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "grossProfit", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // pharmacy_id + product_id + batch_id as tie-breakers keep page
        // boundaries stable when several rows share the same sort value.
        Query query = nativeQuery(reportSql + """
                SELECT pharmacy_name, product_id, product_name, batch_number,
                       sold_quantity, return_quantity, net_quantity,
                       net_sales_amount, net_cost, gross_profit, margin_percentage
                FROM report
                ORDER BY %s %s NULLS LAST, pharmacy_id, product_id, batch_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<GrossProfitReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            GrossProfitReportRowDto dto = new GrossProfitReportRowDto();
            dto.setPharmacyName((String) r[0]);
            dto.setProductId((String) r[1]);
            dto.setProductName((String) r[2]);
            dto.setBatchNumber((String) r[3]);
            dto.setSoldQuantity(toLong(r[4]));
            dto.setReturnQuantity(toLong(r[5]));
            dto.setNetQuantity(toLong(r[6]));
            dto.setNetSalesAmount(toAmount(r[7]));
            dto.setNetCost(toAmount(r[8]));
            dto.setGrossProfit(toAmount(r[9]));
            dto.setMarginPercentage(toAmount(r[10]));
            rows.add(dto);
        }

        return rows;
    }


    private Query nativeQuery(String sql, Map<String, Object> params) {

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }


    // Case-insensitive "contains" on one column, input taken literally.
    private static void appendSearch(
            StringBuilder filters,
            Map<String, Object> params,
            String column,
            String param,
            String value) {

        if (hasText(value)) {
            filters.append(" AND ").append(column).append(" ILIKE :").append(param);
            params.put(param, "%" + escapeLike(value.trim()) + "%");
        }
    }


    private List<String> parseCustomerTypes(List<String> values) {

        List<String> types = new ArrayList<>();

        for (String value : values) {
            CustomerType type;
            try {
                type = CustomerType.valueOf(value.trim().toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException e) {
                type = null;
            }

            if (type == null || !REPORT_CUSTOMER_TYPES.contains(type)) {
                throw new IllegalArgumentException("Invalid customerType: " + value
                        + ". Allowed: " + REPORT_CUSTOMER_TYPES);
            }
            types.add(type.name());
        }

        return types;
    }


    private static BigDecimal percentage(BigDecimal part, BigDecimal total) {

        if (total.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
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
