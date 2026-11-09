package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.GrossProfitSalesBookFilter;
import tiameds.pharmabackend.dto.auditReports.GrossProfitSalesBookPageDto;
import tiameds.pharmabackend.dto.auditReports.GrossProfitSalesBookPharmacyDto;
import tiameds.pharmabackend.dto.auditReports.GrossProfitSalesBookResponse;
import tiameds.pharmabackend.dto.auditReports.GrossProfitSalesBookRowDto;
import tiameds.pharmabackend.dto.auditReports.GrossProfitSalesBookSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.auditReports.GrossProfitSalesBookService;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;
import tiameds.pharmabackend.service.impl.financialReport.export.ExportFormat;
import tiameds.pharmabackend.service.impl.financialReport.export.ReportTableWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrossProfitSalesBookServiceImpl implements GrossProfitSalesBookService {

    private static final String REPORT_NAME = "G.P. ON CONSOLIDATED SALES BOOK";

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;

    // Print / PDF (unpaged) is refused above this many rows. One row per day
    // means even a multi-year range stays well under it.
    private static final int PRINT_ROW_LIMIT = 2000;

    private static final int EXPORT_BATCH_SIZE = 2000;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DISPLAY_MONTH = DateTimeFormatter.ofPattern("MMM-yyyy", Locale.ENGLISH);

    private static final List<String> EXPORT_HEADERS = List.of(
            "Date", "Bill Amount", "Cost", "Gross Profit", "G.P. %");

    private enum GroupBy { DAY, MONTH }

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "date", "period_date",
            "billAmount", "bill_amount",
            "cost", "cost",
            "grossProfit", "gross_profit",
            "grossProfitPercentage", "gross_profit_percentage");

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public GrossProfitSalesBookResponse getGrossProfitSalesBook(
            GrossProfitSalesBookFilter filter,
            UserDetails user) {

        ReportQuery report = prepareReport(filter, user);

        long totalRecords = fetchTotalRecords(report);

        int size;
        int page;

        if (Boolean.TRUE.equals(filter.getUnpaged())) {
            if (totalRecords > PRINT_ROW_LIMIT) {
                throw new IllegalArgumentException(
                        "Too many rows to print (" + totalRecords + "). The limit is " + PRINT_ROW_LIMIT
                                + ". Narrow the period, group by MONTH or download Excel.");
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

        GrossProfitSalesBookPageDto salesBook = new GrossProfitSalesBookPageDto();
        salesBook.setData(fetchPage(report, filter, size, offset));
        salesBook.setPage(page);
        salesBook.setSize(size);
        salesBook.setTotalRecords(totalRecords);
        salesBook.setTotalPages((int) ((totalRecords + size - 1) / size));

        GrossProfitSalesBookResponse response = new GrossProfitSalesBookResponse();
        response.setReportName(REPORT_NAME);
        response.setFromDate(report.fromDate());
        response.setToDate(report.toDate());
        response.setGroupBy(report.groupBy().name());
        response.setPharmacies(pharmacyHeaders(report.pharmacyIds()));
        response.setSummary(fetchSummary(report));
        response.setSalesBook(salesBook);
        response.setGeneratedBy(report.user().getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Runs the count and the KPI totals now, so bad filters and missing
    // access fail before any of the file has been sent.
    @Override
    public ReportExport exportGrossProfitSalesBook(
            GrossProfitSalesBookFilter filter,
            String format,
            UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        ReportQuery report = prepareReport(filter, user);

        long totalRecords = fetchTotalRecords(report);

        GrossProfitSalesBookSummaryDto summary = fetchSummary(report);
        String generatedBy = report.user().getFullName();
        LocalDateTime generatedOn = LocalDateTime.now();
        String pharmacies = String.join(", ", pharmacyHeaders(report.pharmacyIds()).stream()
                .map(GrossProfitSalesBookPharmacyDto::getPharmacyName)
                .toList());

        String period = report.fromDate().format(DISPLAY_DATE) + " to " + report.toDate().format(DISPLAY_DATE);
        // gross-profit-sales-book_2026-09-01_to_2026-09-30.xlsx
        String fileName = "gross-profit-sales-book_" + report.fromDate() + "_to_" + report.toDate()
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

                    writer.title("Gross Profit (GP) on Consolidated Sales Book");
                    writer.line("Report Name", REPORT_NAME);
                    writer.line("Pharmacy", pharmacies);
                    writer.line("Date Range", period);
                    writer.line("Grouped By", report.groupBy().name());
                    writer.line("Generated By", generatedBy);
                    writer.line("Generated On", generatedOn);
                    writer.blank();

                    writer.line("Total Bill Amount", summary.getTotalBillAmount());
                    writer.line("Days With Sales", summary.getDaysWithSales());
                    writer.line("Total Cost", summary.getTotalCost());
                    writer.line("Cost % of Bill Amount", summary.getCostPercentageOfBillAmount());
                    writer.line("Total Gross Profit", summary.getTotalGrossProfit());
                    writer.line("Average Gross Profit Per Day", summary.getAverageGrossProfitPerDay());
                    writer.line("Overall G.P. %", summary.getOverallGrossProfitPercentage());
                    writer.blank();

                    writer.header(EXPORT_HEADERS);

                    for (long offset = 0; offset < totalRecords; offset += EXPORT_BATCH_SIZE) {

                        List<GrossProfitSalesBookRowDto> batch =
                                fetchPage(report, filter, EXPORT_BATCH_SIZE, offset);

                        for (GrossProfitSalesBookRowDto row : batch) {
                            writer.row(Arrays.asList(
                                    row.getPeriodLabel(), row.getBillAmount(), row.getCost(),
                                    row.getGrossProfit(), row.getGrossProfitPercentage()));
                        }

                        if (batch.isEmpty()) {
                            break;
                        }
                    }

                    writer.row(Arrays.asList(
                            "Total: " + totalRecords + (report.groupBy() == GroupBy.MONTH ? " Months" : " Days"),
                            summary.getTotalBillAmount(), summary.getTotalCost(),
                            summary.getTotalGrossProfit(), summary.getOverallGrossProfitPercentage()));

                    writer.finish();
                }
            }
        };
    }


    // The report as the filters define it, shared by the screen, print and
    // export: the user, the resolved period, grouping and pharmacies, and the
    // CTE SQL with its parameters.
    private record ReportQuery(
            UserDetails user,
            LocalDate fromDate,
            LocalDate toDate,
            GroupBy groupBy,
            List<String> pharmacyIds,
            String sql,
            Map<String, Object> params) {
    }


    private ReportQuery prepareReport(GrossProfitSalesBookFilter filter, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate fromDate = filter.getFromDate() != null ? filter.getFromDate() : LocalDate.now();
        LocalDate toDate = filter.getToDate() != null ? filter.getToDate() : fromDate;

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate cannot be before fromDate.");
        }

        GroupBy groupBy = parseGroupBy(filter.getGroupBy());
        List<String> pharmacyIds = resolvePharmacyIds(filter.getPharmacyIds(), persistentUser);

        Map<String, Object> params = new HashMap<>();
        String reportSql = buildReportSql(filter, groupBy, pharmacyIds, fromDate, toDate, params);

        return new ReportQuery(persistentUser, fromDate, toDate, groupBy, pharmacyIds, reportSql, params);
    }


    // Header block of each pharmacy, in the order the ids were given.
    private List<GrossProfitSalesBookPharmacyDto> pharmacyHeaders(List<String> pharmacyIds) {

        Map<String, PharmacyDetails> byId = new HashMap<>();

        for (PharmacyDetails pharmacy : pharmacyDetailsRepository.findAllById(pharmacyIds)) {
            byId.put(pharmacy.getPharmacyId(), pharmacy);
        }

        List<GrossProfitSalesBookPharmacyDto> headers = new ArrayList<>(pharmacyIds.size());

        for (String id : pharmacyIds) {
            PharmacyDetails pharmacy = byId.get(id);

            GrossProfitSalesBookPharmacyDto dto = new GrossProfitSalesBookPharmacyDto();
            dto.setPharmacyId(id);

            if (pharmacy == null) {
                dto.setPharmacyName(id);
            } else {
                dto.setPharmacyName(hasText(pharmacy.getPharmacyName()) ? pharmacy.getPharmacyName() : id);
                dto.setAddress(formatAddress(pharmacy));
                dto.setPhoneNumber(pharmacy.getPharmacyPhone() == null ? null : pharmacy.getPharmacyPhone().toString());
                dto.setEmailAddress(pharmacy.getPharmacyEmail());
                dto.setGstin(pharmacy.getGstNumber());
            }
            headers.add(dto);
        }

        return headers;
    }


    // "12, 100 Feet Road, Indiranagar, Bengaluru, Karnataka 560038"
    private static String formatAddress(PharmacyDetails p) {

        String statePin = String.join(" ", Stream.of(
                        p.getPharmacyState(),
                        p.getPharmacyPincode() == null ? null : p.getPharmacyPincode().toString())
                .filter(GrossProfitSalesBookServiceImpl::hasText)
                .toList());

        String address = String.join(", ", Stream.of(
                        p.getPharmacyBuildingNo(), p.getPharmacyStreet(), p.getPharmacyLandmark(),
                        p.getPharmacyCity(), statePin)
                .filter(GrossProfitSalesBookServiceImpl::hasText)
                .map(String::trim)
                .toList());

        return address.isEmpty() ? null : address;
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
     *   lines  - one row per bill line in the period (positive) and per line of
     *            a Completed sales return dated in the period (negative), so a
     *            return of an older bill counts against the day it happened.
     *            sale_day is set on sale lines only, to count days with sales.
     *   report - lines summed per day (or per month) across the pharmacies.
     *
     * Bill Amount is the line net amount (after discount, incl. GST), what the
     * customer was billed. Cost is the quantity at the batch's purchase price
     * per smallest unit, the unit bill and return quantities are kept in.
     * G.P. % is 0 when the bill amount is not positive (a day of returns only).
     */
    private String buildReportSql(
            GrossProfitSalesBookFilter filter,
            GroupBy groupBy,
            List<String> pharmacyIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        params.put("pharmacyIds", pharmacyIds);
        params.put("fromTs", fromDate.atStartOfDay());
        params.put("toTs", toDate.plusDays(1).atStartOfDay());
        params.put("completedStatus", SalesReturnStatus.COMPLETED.getLabel());

        StringBuilder productFilters = new StringBuilder();

        // gst_percentage is a text column ("12", "12.00", "12%"), so it is
        // reduced to its digits and compared as a number.
        if (!isEmpty(filter.getGstRates())) {
            productFilters.append(" ").append("""
                     AND CAST(NULLIF(regexp_replace(p.gst_percentage, '[^0-9.]', '', 'g'), '') AS NUMERIC)
                         IN (:gstRates)""");
            params.put("gstRates", filter.getGstRates());
        }

        String returnTs = "COALESCE(sr.sales_return_date, sr.created_at)";

        return """
                WITH lines AS (
                    SELECT %1$s                                   AS period_date,
                           CAST(b.created_at AS DATE)             AS sale_day,
                           COALESCE(bd.net_amount, 0)             AS amount,
                           COALESCE(bd.bill_quantity, 0)
                               * COALESCE(CAST(bt.purchase_price_per_unit AS NUMERIC), 0) AS cost
                    FROM pharma_billing_details bd
                    JOIN pharma_billing b ON b.billing_id = bd.billing_id
                    JOIN pharma_product_details p ON p.product_id = bd.product_id
                    LEFT JOIN pharma_batch_details bt ON bt.batch_id = bd.batch_id
                    WHERE b.pharmacy_id IN (:pharmacyIds)
                      AND b.created_at >= :fromTs
                      AND b.created_at <  :toTs
                      %3$s

                    UNION ALL

                    SELECT %2$s                                   AS period_date,
                           CAST(NULL AS DATE)                     AS sale_day,
                           -COALESCE(srd.net_amount, 0)           AS amount,
                           -COALESCE(srd.sales_return_quantity, 0)
                               * COALESCE(CAST(bt.purchase_price_per_unit AS NUMERIC), 0) AS cost
                    FROM pharma_sales_return_details srd
                    JOIN pharma_sales_return sr ON sr.sales_return_id = srd.sales_return_id
                    JOIN pharma_product_details p ON p.product_id = srd.product_id
                    LEFT JOIN pharma_batch_details bt ON bt.batch_id = srd.batch_id
                    WHERE sr.pharmacy_id IN (:pharmacyIds)
                      AND sr.sales_return_status = :completedStatus
                      AND %4$s >= :fromTs
                      AND %4$s <  :toTs
                      %3$s
                ),
                report AS (
                    SELECT period_date,
                           SUM(amount)             AS bill_amount,
                           SUM(cost)               AS cost,
                           SUM(amount) - SUM(cost) AS gross_profit,
                           CASE WHEN SUM(amount) > 0
                                THEN (SUM(amount) - SUM(cost)) * 100 / SUM(amount)
                                ELSE 0
                           END                     AS gross_profit_percentage
                    FROM lines
                    GROUP BY period_date
                )
                """.formatted(
                periodExpression(groupBy, "b.created_at"),
                periodExpression(groupBy, returnTs),
                productFilters,
                returnTs);
    }


    // Only ever built from the enum and fixed column names, never user input.
    private static String periodExpression(GroupBy groupBy, String timestampColumn) {
        return groupBy == GroupBy.MONTH
                ? "CAST(DATE_TRUNC('month', " + timestampColumn + ") AS DATE)"
                : "CAST(" + timestampColumn + " AS DATE)";
    }


    private GrossProfitSalesBookSummaryDto fetchSummary(ReportQuery report) {

        Object[] row = (Object[]) nativeQuery(report.sql() + """
                SELECT COALESCE(SUM(bill_amount), 0),
                       COALESCE(SUM(cost), 0),
                       COALESCE(SUM(gross_profit), 0),
                       (SELECT COUNT(DISTINCT sale_day) FROM lines)
                FROM report
                """, report.params()).getSingleResult();

        BigDecimal billAmount = toAmount(row[0]);
        BigDecimal cost = toAmount(row[1]);
        BigDecimal grossProfit = toAmount(row[2]);
        long daysWithSales = toLong(row[3]);

        GrossProfitSalesBookSummaryDto summary = new GrossProfitSalesBookSummaryDto();
        summary.setTotalBillAmount(billAmount);
        summary.setDaysWithSales(daysWithSales);
        summary.setTotalCost(cost);
        summary.setCostPercentageOfBillAmount(percentage(cost, billAmount));
        summary.setTotalGrossProfit(grossProfit);
        summary.setAverageGrossProfitPerDay(daysWithSales == 0
                ? BigDecimal.ZERO.setScale(2)
                : grossProfit.divide(BigDecimal.valueOf(daysWithSales), 2, RoundingMode.HALF_UP));
        summary.setOverallGrossProfitPercentage(percentage(grossProfit, billAmount));

        return summary;
    }


    private long fetchTotalRecords(ReportQuery report) {

        return toLong(nativeQuery(report.sql() + "SELECT COUNT(*) FROM report", report.params())
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<GrossProfitSalesBookRowDto> fetchPage(
            ReportQuery report,
            GrossProfitSalesBookFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "date", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "desc".equalsIgnoreCase(filter.getSortDir()) ? "DESC" : "ASC";

        // period_date as tie-breaker keeps page boundaries stable when several
        // rows share the same sort value.
        Query query = nativeQuery(report.sql() + """
                SELECT period_date, bill_amount, cost, gross_profit, gross_profit_percentage
                FROM report
                ORDER BY %s %s NULLS LAST, period_date
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), report.params());

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<GrossProfitSalesBookRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            LocalDate date = toLocalDate(r[0]);

            GrossProfitSalesBookRowDto dto = new GrossProfitSalesBookRowDto();
            dto.setDate(date);
            dto.setPeriodLabel(date == null ? null
                    : date.format(report.groupBy() == GroupBy.MONTH ? DISPLAY_MONTH : DISPLAY_DATE));
            dto.setBillAmount(toAmount(r[1]));
            dto.setCost(toAmount(r[2]));
            dto.setGrossProfit(toAmount(r[3]));
            dto.setGrossProfitPercentage(toAmount(r[4]));
            rows.add(dto);
        }

        return rows;
    }


    private Query nativeQuery(String sql, Map<String, Object> params) {

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }


    private static GroupBy parseGroupBy(String value) {

        if (!hasText(value)) {
            return GroupBy.DAY;
        }
        try {
            return GroupBy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid groupBy: " + value
                    + ". Allowed: " + Arrays.toString(GroupBy.values()));
        }
    }


    // part * 100 / whole, 0 when whole is not positive.
    private static BigDecimal percentage(BigDecimal part, BigDecimal whole) {

        if (whole.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }


    private static LocalDate toLocalDate(Object value) {

        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate d) {
            return d;
        }
        if (value instanceof Date d) {
            return d.toLocalDate();
        }
        if (value instanceof java.util.Date d) {
            return new Date(d.getTime()).toLocalDate();
        }
        return LocalDate.parse(value.toString().substring(0, 10));
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


    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
