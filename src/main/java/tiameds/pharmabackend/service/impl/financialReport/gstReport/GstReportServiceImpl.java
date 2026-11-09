package tiameds.pharmabackend.service.impl.financialReport.gstReport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstLedgerDto;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstLedgerRowDto;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstLedgerSectionDto;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstLocationDto;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstRateGroupDto;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstRegistrationDto;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstReportHeaderDto;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstSalesPurchaseKpiDto;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstSalesPurchaseSummaryResponse;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstSummaryDto;
import tiameds.pharmabackend.dto.financialReport.gstReport.GstSummaryRowDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.financialReport.export.ReportExport;
import tiameds.pharmabackend.service.financialReport.gstReport.GstReportService;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport;
import tiameds.pharmabackend.service.impl.financialReport.export.ExportFormat;
import tiameds.pharmabackend.service.impl.financialReport.export.ReportTableWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.hasText;
import static tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.trimmed;

/**
 * GST Sales &amp; Purchase Summary: taxable value and GST of sales and
 * purchases per month and GST rate, less returns, and the monthly Net GST.
 * <p>
 * Sources (BR-04, BR-05, BR-13): pharmacy bills and completed sales returns;
 * supplier purchases and confirmed purchase returns (active line revisions
 * only). Warehouse-to-pharmacy distributions are never read, so stock moving
 * inside the organization is not a sale or purchase and nothing is counted
 * twice (BR-12, BR-14). Drafts and cancelled returns are left out (BR-16).
 * <p>
 * Months: a bill by its date, a purchase by its invoice date, and a return by
 * its return date, so a credit / debit note falls in the month it was issued.
 * <p>
 * Taxable value: a bill line's gross amount (already after discount) and a
 * purchase line's gross amount less discount. GST rate: the purchase line's
 * stored rate; a bill or sales return line stores none, so it is worked out
 * from the line's own GST and taxable value (the rate actually charged),
 * falling back to the product's rate when the taxable value is zero.
 * <p>
 * Every transaction is Local for now: Central (inter-state) is not captured,
 * so the Central sections are always empty. Separate credit / debit notes
 * and government tax refunds are not recorded either: CR/DR Note is the
 * returns' taxable value and Tax Refund is zero.
 * <p>
 * Amounts are summed as stored and never rounded here (BR-15).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GstReportServiceImpl implements GstReportService {

    private static final String REPORT_NAME = "GST Sales & Purchase Summary";

    private static final String LOCAL = "LOCAL";
    private static final String CENTRAL = "CENTRAL";

    private static final String SALE = "SALE";
    private static final String PURCHASE = "PURCHASE";

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    private static final List<String> LEDGER_HEADERS_SALES = List.of(
            "Sales Ledger", "Net Sale", "Net Tax", "Sale Amount", "Tax Amount",
            "CR/DR Note", "Tax Return", "Tax Refund");

    private static final List<String> LEDGER_HEADERS_PURCHASES = List.of(
            "Purchase Ledger", "Net Purc.", "Net Tax", "Purc. Amount", "Tax Amount",
            "DR/CR Note", "Tax Return", "Tax Refund");

    private final FinancialReportSupport support;
    private final LocationContextResolver locationContextResolver;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public GstSalesPurchaseSummaryResponse getSalesPurchaseSummary(FinancialReportFilter filter, UserDetails user) {

        Report report = buildReport(filter, user);

        return new GstSalesPurchaseSummaryResponse(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                report.header(),
                report.kpi(),
                report.salesLedger(),
                report.purchaseLedger(),
                report.gstSummary());
    }


    @Override
    public List<GstRegistrationDto> getGstRegistrations(FinancialReportFilter filter, UserDetails user) {

        Locations locations = resolveLocations(filter, user);

        return registrations(locations.pharmacies(), locations.warehouses());
    }


    // Builds the whole report now, so bad filters and missing access fail
    // before any of the file has been sent.
    @Override
    public ReportExport exportSalesPurchaseSummary(String format, FinancialReportFilter filter, UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        Report report = buildReport(filter, user);
        String generatedBy = FinancialReportSupport.generatedBy(user);
        LocalDateTime generatedOn = LocalDateTime.now();

        GstReportHeaderDto header = report.header();
        String fileName = "gst-sales-purchase-summary_" + header.getFromDate() + "_to_" + header.getToDate()
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

                    writer.title(REPORT_NAME);

                    for (GstRegistrationDto registration : header.getRegistrations()) {
                        writer.line("GSTIN", registration.getGstin());
                        writer.line("Dealer Name", registration.getDealerName());
                        writer.line("Address", registration.getAddress());
                        writer.line("Locations", String.join(", ", registration.getLocations().stream()
                                .map(GstLocationDto::getLocationName)
                                .toList()));
                    }

                    writer.line("Reporting Period", header.getFromDate().format(DISPLAY_DATE)
                            + " to " + header.getToDate().format(DISPLAY_DATE));
                    writer.line("Transaction Type", header.getTransactionType());
                    writer.line("GST Classification", header.getGstClassification());
                    writer.line("GST Rate", header.getGstRates().isEmpty()
                            ? "All" : String.join(", ", header.getGstRates()));
                    writer.line("Generated By", generatedBy);
                    writer.line("Generated On", generatedOn);
                    writer.blank();

                    GstSalesPurchaseKpiDto kpi = report.kpi();
                    writer.line("Net Sale", kpi.getNetSale());
                    writer.line("Output GST (Net Tax on Sales)", kpi.getOutputGst());
                    writer.line("Net Purchase", kpi.getNetPurchase());
                    writer.line("Input GST (Net Tax on Purchase)", kpi.getInputGst());
                    writer.line("Net GST", kpi.getNetGst());

                    if (report.salesLedger() != null) {
                        writer.blank();
                        writeLedger(writer, LEDGER_HEADERS_SALES, report.salesLedger());
                    }

                    if (report.purchaseLedger() != null) {
                        writer.blank();
                        writeLedger(writer, LEDGER_HEADERS_PURCHASES, report.purchaseLedger());
                    }

                    if (report.gstSummary() != null) {
                        writer.blank();
                        writer.header(List.of("GST Summary - Month", "Output GST", "Input GST", "Net GST"));

                        for (GstSummaryRowDto row : report.gstSummary().getMonths()) {
                            writer.row(Arrays.asList(row.getLabel(), row.getOutputGst(), row.getInputGst(),
                                    row.getNetGst()));
                        }

                        GstSummaryRowDto total = report.gstSummary().getGrandTotal();
                        writer.row(Arrays.asList(total.getLabel(), total.getOutputGst(), total.getInputGst(),
                                total.getNetGst()));
                    }

                    writer.finish();
                }
            }
        };
    }


    private static void writeLedger(ReportTableWriter writer, List<String> headers, GstLedgerDto ledger)
            throws IOException {

        writer.header(headers);

        for (GstLedgerSectionDto section : ledger.getSections()) {
            writer.row(List.of(section.getTitle()));

            for (GstRateGroupDto group : section.getRateGroups()) {
                writer.row(List.of(group.getTitle()));

                for (GstLedgerRowDto row : group.getMonths()) {
                    writer.row(ledgerValues(row));
                }

                writer.row(ledgerValues(group.getSubtotal()));
            }
        }

        writer.row(ledgerValues(ledger.getTotal()));
    }


    private static List<Object> ledgerValues(GstLedgerRowDto row) {

        return Arrays.asList(row.getLabel(), row.getNetAmount(), row.getNetTax(), row.getAmount(),
                row.getTaxAmount(), row.getCrDrNote(), row.getTaxReturn(), row.getTaxRefund());
    }


    // ---------------------------------------------------------------- report

    private record Report(
            GstReportHeaderDto header,
            GstSalesPurchaseKpiDto kpi,
            GstLedgerDto salesLedger,
            GstLedgerDto purchaseLedger,
            GstSummaryDto gstSummary) {
    }


    private Report buildReport(FinancialReportFilter filter, UserDetails user) {

        support.resolveBoundedDateRange(filter);

        LocalDate fromDate = filter.getDate() != null ? filter.getDate() : filter.getFromDate();
        LocalDate toDate = filter.getDate() != null ? filter.getDate() : filter.getToDate();

        String transactionType = parseTransactionType(filter.getTransactionType());
        String classification = parseClassification(filter.getGstClassification());
        Set<BigDecimal> rates = parseRates(filter.getGstPercentages());

        boolean includeSales = !"PURCHASES".equals(transactionType);
        boolean includePurchases = !"SALES".equals(transactionType);

        Locations locations = resolveLocations(filter, user);

        // kind -> rate -> month -> amounts; every transaction is Local for now.
        Map<String, TreeMap<BigDecimal, TreeMap<YearMonth, Amounts>>> totals =
                fetchTotals(locations, fromDate, toDate, includeSales, includePurchases, rates);

        GstLedgerDto salesLedger = includeSales
                ? ledger(SALE, totals.getOrDefault(SALE, new TreeMap<>()), classification)
                : null;

        GstLedgerDto purchaseLedger = includePurchases
                ? ledger(PURCHASE, totals.getOrDefault(PURCHASE, new TreeMap<>()), classification)
                : null;

        GstSummaryDto gstSummary = includeSales && includePurchases
                ? gstSummary(fromDate, toDate,
                monthlyNetTax(totals.get(SALE), classification),
                monthlyNetTax(totals.get(PURCHASE), classification))
                : null;

        GstSalesPurchaseKpiDto kpi = new GstSalesPurchaseKpiDto();

        if (salesLedger != null) {
            kpi.setNetSale(salesLedger.getTotal().getNetAmount());
            kpi.setOutputGst(salesLedger.getTotal().getNetTax());
        }

        if (purchaseLedger != null) {
            kpi.setNetPurchase(purchaseLedger.getTotal().getNetAmount());
            kpi.setInputGst(purchaseLedger.getTotal().getNetTax());
        }

        if (gstSummary != null) {
            kpi.setNetGst(gstSummary.getGrandTotal().getNetGst());
        }

        GstReportHeaderDto header = new GstReportHeaderDto(
                REPORT_NAME,
                fromDate,
                toDate,
                transactionType,
                classification,
                rates.stream().map(GstReportServiceImpl::rateText).toList(),
                registrations(locations.pharmacies(), locations.warehouses()));

        return new Report(header, kpi, salesLedger, purchaseLedger, gstSummary);
    }


    // ---------------------------------------------------------------- ledgers

    private GstLedgerDto ledger(
            String kind,
            TreeMap<BigDecimal, TreeMap<YearMonth, Amounts>> byRate,
            String classification) {

        boolean sales = SALE.equals(kind);
        List<GstLedgerSectionDto> sections = new ArrayList<>();
        Amounts grandTotal = Amounts.zero();

        for (String section : List.of(LOCAL, CENTRAL)) {

            if (!"ALL".equals(classification) && !classification.equals(section)) {
                continue;
            }

            // Central (inter-state) is not captured yet: always empty.
            TreeMap<BigDecimal, TreeMap<YearMonth, Amounts>> data = LOCAL.equals(section) ? byRate : new TreeMap<>();
            String place = LOCAL.equals(section) ? "LOCAL" : "CENTRAL";
            List<GstRateGroupDto> groups = new ArrayList<>();

            for (Map.Entry<BigDecimal, TreeMap<YearMonth, Amounts>> rate : data.entrySet()) {

                String title = "GST " + place + (sales ? " SALES " : " PURCHASE ") + rateText(rate.getKey()) + "%";
                List<GstLedgerRowDto> months = new ArrayList<>();
                Amounts subtotal = Amounts.zero();

                for (Map.Entry<YearMonth, Amounts> month : rate.getValue().entrySet()) {
                    months.add(row("MONTH", month.getKey().toString(), month.getKey().format(MONTH_LABEL),
                            month.getValue()));
                    subtotal = subtotal.add(month.getValue());
                }

                groups.add(new GstRateGroupDto(rate.getKey(), title, months,
                        row("SUBTOTAL", null, title + " - Subtotal", subtotal)));
                grandTotal = grandTotal.add(subtotal);
            }

            sections.add(new GstLedgerSectionDto(section, (sales ? "SALES " : "PURCHASE ") + place, groups));
        }

        return new GstLedgerDto(sections, row("TOTAL", null, sales ? "TOTAL SALES" : "TOTAL PURCHASES", grandTotal));
    }


    private static GstLedgerRowDto row(String rowType, String month, String label, Amounts amounts) {

        return new GstLedgerRowDto(
                rowType,
                month,
                label,
                amounts.amount().subtract(amounts.returnAmount()),
                amounts.tax().subtract(amounts.returnTax()),
                amounts.amount(),
                amounts.tax(),
                amounts.returnAmount(),
                amounts.returnTax(),
                BigDecimal.ZERO);
    }


    // Net tax per month of a ledger, over the sections the classification
    // filter keeps (Central is always empty, so only Local adds anything).
    private static Map<YearMonth, BigDecimal> monthlyNetTax(
            TreeMap<BigDecimal, TreeMap<YearMonth, Amounts>> byRate,
            String classification) {

        Map<YearMonth, BigDecimal> netTax = new HashMap<>();

        if (byRate == null || CENTRAL.equals(classification)) {
            return netTax;
        }

        for (TreeMap<YearMonth, Amounts> months : byRate.values()) {
            for (Map.Entry<YearMonth, Amounts> month : months.entrySet()) {
                Amounts a = month.getValue();
                netTax.merge(month.getKey(), a.tax().subtract(a.returnTax()), BigDecimal::add);
            }
        }

        return netTax;
    }


    // Every month of the period, zero when nothing happened in it.
    private static GstSummaryDto gstSummary(
            LocalDate fromDate,
            LocalDate toDate,
            Map<YearMonth, BigDecimal> output,
            Map<YearMonth, BigDecimal> input) {

        List<GstSummaryRowDto> months = new ArrayList<>();
        BigDecimal totalOutput = BigDecimal.ZERO;
        BigDecimal totalInput = BigDecimal.ZERO;

        for (YearMonth month = YearMonth.from(fromDate); !month.isAfter(YearMonth.from(toDate));
             month = month.plusMonths(1)) {

            BigDecimal out = output.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal in = input.getOrDefault(month, BigDecimal.ZERO);

            months.add(new GstSummaryRowDto("MONTH", month.toString(), month.format(MONTH_LABEL),
                    out, in, out.subtract(in)));
            totalOutput = totalOutput.add(out);
            totalInput = totalInput.add(in);
        }

        return new GstSummaryDto(months, new GstSummaryRowDto("TOTAL", null, "Grand Total",
                totalOutput, totalInput, totalOutput.subtract(totalInput)));
    }


    // ---------------------------------------------------------------- query

    private record Amounts(BigDecimal amount, BigDecimal tax, BigDecimal returnAmount, BigDecimal returnTax) {

        static Amounts zero() {
            return new Amounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Amounts add(Amounts other) {
            return new Amounts(
                    amount.add(other.amount),
                    tax.add(other.tax),
                    returnAmount.add(other.returnAmount),
                    returnTax.add(other.returnTax));
        }
    }


    // A text rate as stored ("12%", "5", "Exempted") reduced to its number;
    // null when it has no digits.
    private static String parsedRate(String column) {
        return "ROUND(CAST(NULLIF(regexp_replace(" + column + ", '[^0-9.]', '', 'g'), '') AS NUMERIC), 2)";
    }


    // The rate actually charged on a line, from its GST and taxable value,
    // to the nearest whole percent (absorbs paise rounding: 4.97 -> 5).
    private static String derivedRate(String gst, String taxable) {
        return "ROUND(CAST(" + gst + " * 100 / NULLIF(" + taxable + ", 0) AS NUMERIC), 0)";
    }


    @SuppressWarnings("unchecked")
    private Map<String, TreeMap<BigDecimal, TreeMap<YearMonth, Amounts>>> fetchTotals(
            Locations locations,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeSales,
            boolean includePurchases,
            Set<BigDecimal> rates) {

        Map<String, Object> params = new HashMap<>();
        params.put("fromTs", fromDate.atStartOfDay());
        params.put("toTs", toDate.plusDays(1).atStartOfDay());

        List<String> branches = new ArrayList<>();

        // Sales happen at pharmacies only.
        if (includeSales && !locations.pharmacyIds().isEmpty()) {

            params.put("pharmacyIds", locations.pharmacyIds());
            params.put("completedStatus", SalesReturnStatus.COMPLETED.getLabel());

            branches.add("""
                    SELECT 'SALE' AS kind,
                           b.created_at AS ts,
                           COALESCE(%1$s, %2$s, 0) AS rate,
                           COALESCE(bd.gross_amount, 0) AS amount,
                           COALESCE(bd.gst_amount, 0) AS tax,
                           CAST(0 AS NUMERIC) AS return_amount,
                           CAST(0 AS NUMERIC) AS return_tax
                    FROM pharma_billing_details bd
                    JOIN pharma_billing b ON b.billing_id = bd.billing_id
                    LEFT JOIN pharma_product_details p ON p.product_id = bd.product_id
                    WHERE b.pharmacy_id IN (:pharmacyIds)
                      AND b.created_at >= :fromTs
                      AND b.created_at < :toTs
                    """.formatted(
                    derivedRate("bd.gst_amount", "bd.gross_amount"),
                    parsedRate("p.gst_percentage")));

            branches.add("""
                    SELECT 'SALE' AS kind,
                           COALESCE(sr.sales_return_date, sr.created_at) AS ts,
                           COALESCE(%1$s, %2$s, 0) AS rate,
                           CAST(0 AS NUMERIC) AS amount,
                           CAST(0 AS NUMERIC) AS tax,
                           COALESCE(srd.gross_amount, 0) AS return_amount,
                           COALESCE(srd.gst_amount, 0) AS return_tax
                    FROM pharma_sales_return_details srd
                    JOIN pharma_sales_return sr ON sr.sales_return_id = srd.sales_return_id
                    LEFT JOIN pharma_product_details p ON p.product_id = srd.product_id
                    WHERE sr.pharmacy_id IN (:pharmacyIds)
                      AND sr.sales_return_status = :completedStatus
                      AND COALESCE(sr.sales_return_date, sr.created_at) >= :fromTs
                      AND COALESCE(sr.sales_return_date, sr.created_at) < :toTs
                    """.formatted(
                    derivedRate("srd.gst_amount", "srd.gross_amount"),
                    parsedRate("p.gst_percentage")));
        }

        if (includePurchases && !locations.isEmpty()) {

            params.put("confirmedStatus", PurchaseReturnStatus.CONFIRMED.name());

            branches.add("""
                    SELECT 'PURCHASE' AS kind,
                           COALESCE(pu.invoice_date, pu.created_at) AS ts,
                           COALESCE(%2$s, %3$s, 0) AS rate,
                           COALESCE(pd.gross_amount, 0) - COALESCE(pd.discount, 0) AS amount,
                           COALESCE(pd.gst, 0) AS tax,
                           CAST(0 AS NUMERIC) AS return_amount,
                           CAST(0 AS NUMERIC) AS return_tax
                    FROM pharma_purchase_details pd
                    JOIN pharma_purchase pu ON pu.purchase_id = pd.purchase_id
                    WHERE (%1$s)
                      AND COALESCE(pu.invoice_date, pu.created_at) >= :fromTs
                      AND COALESCE(pu.invoice_date, pu.created_at) < :toTs
                    """.formatted(
                    locationScope("pu", locations, params),
                    parsedRate("pd.gst_percentage"),
                    derivedRate("pd.gst", "(COALESCE(pd.gross_amount, 0) - COALESCE(pd.discount, 0))")));

            // A return line takes the rate of the purchase line it returns
            // (same product, preferably the same batch).
            branches.add("""
                    SELECT 'PURCHASE' AS kind,
                           COALESCE(pr.purchase_return_date, pr.created_at) AS ts,
                           COALESCE(%2$s, %3$s, %4$s, 0) AS rate,
                           CAST(0 AS NUMERIC) AS amount,
                           CAST(0 AS NUMERIC) AS tax,
                           COALESCE(prd.gross_amount, 0) - COALESCE(prd.discount_amount, 0) AS return_amount,
                           COALESCE(prd.gst_amount, 0) AS return_tax
                    FROM pharma_purchase_return_details prd
                    JOIN pharma_purchase_return pr ON pr.purchase_return_id = prd.purchase_return_id
                    LEFT JOIN pharma_product_details p ON p.product_id = prd.product_id
                    LEFT JOIN LATERAL (
                        SELECT pd.gst_percentage
                        FROM pharma_purchase_details pd
                        WHERE pd.purchase_id = pr.purchase_id
                          AND pd.product_id = prd.product_id
                        ORDER BY CASE WHEN pd.batch_id IS NOT DISTINCT FROM prd.batch_id THEN 0 ELSE 1 END,
                                 pd.purchase_details_id
                        LIMIT 1
                    ) ol ON TRUE
                    WHERE (%1$s)
                      AND pr.status = :confirmedStatus
                      AND COALESCE(prd.is_active, TRUE)
                      AND COALESCE(pr.purchase_return_date, pr.created_at) >= :fromTs
                      AND COALESCE(pr.purchase_return_date, pr.created_at) < :toTs
                    """.formatted(
                    locationScope("pr", locations, params),
                    parsedRate("ol.gst_percentage"),
                    derivedRate("prd.gst_amount",
                            "(COALESCE(prd.gross_amount, 0) - COALESCE(prd.discount_amount, 0))"),
                    parsedRate("p.gst_percentage")));
        }

        Map<String, TreeMap<BigDecimal, TreeMap<YearMonth, Amounts>>> totals = new HashMap<>();

        if (branches.isEmpty()) {
            return totals;
        }

        String sql = "WITH lines AS (\n" + String.join("UNION ALL\n", branches) + """
                )
                SELECT kind,
                       date_trunc('month', ts) AS month,
                       rate,
                       SUM(amount),
                       SUM(tax),
                       SUM(return_amount),
                       SUM(return_tax)
                FROM lines
                GROUP BY kind, date_trunc('month', ts), rate
                """;

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);

        for (Object[] r : (List<Object[]>) query.getResultList()) {

            BigDecimal rate = toAmount(r[2]);

            if (!rates.isEmpty() && rates.stream().noneMatch(selected -> selected.compareTo(rate) == 0)) {
                continue;
            }

            Amounts amounts = new Amounts(toAmount(r[3]), toAmount(r[4]), toAmount(r[5]), toAmount(r[6]));

            totals.computeIfAbsent((String) r[0], k -> new TreeMap<>(BigDecimal::compareTo))
                    .computeIfAbsent(rate, k -> new TreeMap<>())
                    .merge(toYearMonth(r[1]), amounts, Amounts::add);
        }

        return totals;
    }


    // An empty list in IN (...) is invalid SQL, so each location kind is
    // only included when it has ids.
    private static String locationScope(String alias, Locations locations, Map<String, Object> params) {

        List<String> scope = new ArrayList<>();

        if (!locations.pharmacyIds().isEmpty()) {
            params.put("pharmacyIds", locations.pharmacyIds());
            scope.add(alias + ".pharmacy_id IN (:pharmacyIds)");
        }

        if (!locations.warehouseIds().isEmpty()) {
            params.put("warehouseIds", locations.warehouseIds());
            scope.add(alias + ".warehouse_id IN (:warehouseIds)");
        }

        return String.join(" OR ", scope);
    }


    // ---------------------------------------------------------------- locations

    private record Locations(List<PharmacyDetails> pharmacies, List<Warehouse> warehouses) {

        List<String> pharmacyIds() {
            return pharmacies.stream().map(PharmacyDetails::getPharmacyId).toList();
        }

        List<String> warehouseIds() {
            return warehouses.stream().map(Warehouse::getWarehouseId).toList();
        }

        boolean isEmpty() {
            return pharmacies.isEmpty() && warehouses.isEmpty();
        }
    }


    /*
     * The pharmacies / warehouses the report covers, each one the user may use:
     * the pharmacyIds / warehouseIds multi-selects when either is sent, otherwise
     * the location of the request (the X-Warehouse-Id warehouse for a warehouse
     * user, else the X-Pharmacy-Id pharmacy). The gstin filter then keeps only
     * the locations filing under that GSTIN.
     */
    private Locations resolveLocations(FinancialReportFilter filter, UserDetails user) {

        List<String> pharmacyIds = trimmed(filter.getPharmacyIds());
        List<String> warehouseIds = trimmed(filter.getWarehouseIds());

        if (pharmacyIds.isEmpty() && warehouseIds.isEmpty()) {

            LocationContext location = locationContextResolver.resolve(user);

            if (location.isWarehouse()) {
                warehouseIds = List.of(location.getLocationId());
            } else {
                pharmacyIds = support.resolvePharmacyIds(filter, user);
            }
        } else {
            pharmacyIds = pharmacyIds.isEmpty() ? List.of() : support.resolvePharmacyIds(filter, user);
            warehouseIds = checkWarehouseAccess(warehouseIds, user);
        }

        List<PharmacyDetails> pharmacies = inOrder(pharmacyIds,
                pharmacyDetailsRepository.findAllById(pharmacyIds), PharmacyDetails::getPharmacyId);
        List<Warehouse> warehouses = inOrder(warehouseIds,
                warehouseRepository.findAllById(warehouseIds), Warehouse::getWarehouseId);

        if (hasText(filter.getGstin())) {
            String gstin = filter.getGstin().trim();
            pharmacies = pharmacies.stream().filter(p -> gstin.equalsIgnoreCase(gstinOf(p))).toList();
            warehouses = warehouses.stream().filter(w -> gstin.equalsIgnoreCase(gstinOf(w))).toList();
        }

        return new Locations(pharmacies, warehouses);
    }


    // A warehouse the user is mapped to, or any in their organization for a
    // SUPER ADMIN.
    private List<String> checkWarehouseAccess(List<String> warehouseIds, UserDetails user) {

        if (warehouseIds.isEmpty()) {
            return warehouseIds;
        }

        Set<String> mapped = new TreeSet<>(userDetailsRepository.findWarehouseIdsByUserId(user.getUserId()));
        boolean superAdmin = locationContextResolver.isSuperAdmin(user);

        List<String> denied = warehouseIds.stream()
                .filter(id -> !mapped.contains(id)
                        && !(superAdmin && locationContextResolver.warehouseInUserOrganization(id, user)))
                .toList();

        if (!denied.isEmpty()) {
            throw new AccessDeniedException(
                    "You are not authorized to use these warehouses: " + String.join(", ", denied));
        }

        return warehouseIds;
    }


    private static <T> List<T> inOrder(List<String> ids, Iterable<T> found, java.util.function.Function<T, String> id) {

        Map<String, T> byId = new HashMap<>();
        found.forEach(item -> byId.put(id.apply(item), item));

        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }


    // Grouped by GSTIN, in the order the locations were selected.
    private static List<GstRegistrationDto> registrations(List<PharmacyDetails> pharmacies, List<Warehouse> warehouses) {

        Map<String, List<GstLocationDto>> locations = new LinkedHashMap<>();
        Map<String, String> dealerNames = new HashMap<>();

        for (PharmacyDetails pharmacy : pharmacies) {
            String key = Objects.toString(gstinOf(pharmacy), "");
            locations.computeIfAbsent(key, k -> new ArrayList<>()).add(new GstLocationDto(
                    "PHARMACY", pharmacy.getPharmacyId(), pharmacy.getPharmacyName(), address(pharmacy)));
            dealerNames.putIfAbsent(key, dealerName(pharmacy.getOrganization(), pharmacy.getPharmacyName()));
        }

        for (Warehouse warehouse : warehouses) {
            String key = Objects.toString(gstinOf(warehouse), "");
            locations.computeIfAbsent(key, k -> new ArrayList<>()).add(new GstLocationDto(
                    "WAREHOUSE", warehouse.getWarehouseId(), warehouse.getWarehouseName(),
                    warehouse.getWarehouseAddress()));
            dealerNames.putIfAbsent(key, dealerName(warehouse.getOrganization(), warehouse.getWarehouseName()));
        }

        return locations.entrySet().stream()
                .map(entry -> new GstRegistrationDto(
                        entry.getKey().isEmpty() ? null : entry.getKey(),
                        dealerNames.get(entry.getKey()),
                        entry.getValue().get(0).getAddress(),
                        entry.getValue()))
                .toList();
    }


    private static String gstinOf(PharmacyDetails pharmacy) {

        if (hasText(pharmacy.getGstNumber())) {
            return pharmacy.getGstNumber().trim();
        }

        return organizationGstin(pharmacy.getOrganization());
    }


    private static String gstinOf(Warehouse warehouse) {
        return organizationGstin(warehouse.getOrganization());
    }


    private static String organizationGstin(PharmacyOrganization organization) {

        return organization != null && hasText(organization.getGstNumber())
                ? organization.getGstNumber().trim()
                : null;
    }


    private static String dealerName(PharmacyOrganization organization, String locationName) {

        return organization != null && hasText(organization.getOrganizationName())
                ? organization.getOrganizationName()
                : locationName;
    }


    private static String address(PharmacyDetails pharmacy) {

        String joined = String.join(", ", Stream.of(
                        pharmacy.getPharmacyBuildingNo(),
                        pharmacy.getPharmacyStreet(),
                        pharmacy.getPharmacyLandmark(),
                        pharmacy.getPharmacyCity(),
                        pharmacy.getPharmacyTaluka(),
                        pharmacy.getPharmacyDistricts(),
                        pharmacy.getPharmacyState(),
                        pharmacy.getPharmacyPincode() == null ? null : pharmacy.getPharmacyPincode().toString())
                .filter(FinancialReportSupport::hasText)
                .map(String::trim)
                .toList());

        return joined.isEmpty() ? null : joined;
    }


    // ---------------------------------------------------------------- filters

    private static String parseTransactionType(String value) {

        if (!hasText(value)) {
            return "ALL";
        }

        return switch (value.trim().toUpperCase()) {
            case "ALL" -> "ALL";
            case "SALES", "SALE" -> "SALES";
            case "PURCHASES", "PURCHASE" -> "PURCHASES";
            default -> throw new IllegalArgumentException(
                    "Unknown transactionType: " + value + ". Use ALL, SALES or PURCHASES.");
        };
    }


    private static String parseClassification(String value) {

        if (!hasText(value)) {
            return "ALL";
        }

        return switch (value.trim().toUpperCase()) {
            case "ALL" -> "ALL";
            case "LOCAL" -> LOCAL;
            case "CENTRAL" -> CENTRAL;
            default -> throw new IllegalArgumentException(
                    "Unknown gstClassification: " + value + ". Use ALL, LOCAL or CENTRAL.");
        };
    }


    // "5", "5%", "18.00" -> numbers; "Exempted" (no digits) -> 0.
    private static Set<BigDecimal> parseRates(List<String> values) {

        Set<BigDecimal> rates = new TreeSet<>(BigDecimal::compareTo);

        for (String value : trimmed(values)) {
            String digits = value.replaceAll("[^0-9.]", "");
            try {
                rates.add(digits.isEmpty() ? BigDecimal.ZERO : new BigDecimal(digits));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid GST rate: " + value + ".");
            }
        }

        return rates;
    }


    private static String rateText(BigDecimal rate) {
        return rate.stripTrailingZeros().toPlainString();
    }


    private static BigDecimal toAmount(Object value) {

        if (value == null) {
            return BigDecimal.ZERO;
        }

        return value instanceof BigDecimal amount ? amount : new BigDecimal(value.toString());
    }


    private static YearMonth toYearMonth(Object value) {

        if (value instanceof LocalDateTime dateTime) {
            return YearMonth.from(dateTime);
        }

        if (value instanceof Timestamp timestamp) {
            return YearMonth.from(timestamp.toLocalDateTime());
        }

        if (value instanceof java.time.OffsetDateTime dateTime) {
            return YearMonth.from(dateTime);
        }

        throw new IllegalStateException("Unexpected month value: " + value);
    }
}
