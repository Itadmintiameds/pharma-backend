package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.InventorySalesReportFilter;
import tiameds.pharmabackend.dto.auditReports.InventorySalesReportPageDto;
import tiameds.pharmabackend.dto.auditReports.InventorySalesReportResponse;
import tiameds.pharmabackend.dto.auditReports.InventorySalesReportRowDto;
import tiameds.pharmabackend.dto.auditReports.InventorySalesReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.auditReports.AuditReportsService;
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
public class AuditReportsServiceImpl implements AuditReportsService {

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
            "Pharmacy", "Product Name",
            "Sale Quantity", "Return Quantity", "Net Sale Quantity",
            "Sale Amount", "Return Amount", "Net Sales");

    // Patient types offered on the report filter (IP / OP / Walk-In / Day Care).
    private static final Set<CustomerType> REPORT_PATIENT_TYPES = EnumSet.of(
            CustomerType.IP_PATIENT,
            CustomerType.OP_PATIENT,
            CustomerType.WALK_IN,
            CustomerType.DAYCARE);

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "pharmacyName", "pharmacy_name",
            "productName", "product_name",
            "saleQuantity", "sale_quantity",
            "returnQuantity", "return_quantity",
            "netSaleQuantity", "net_sale_quantity",
            "saleAmount", "sale_amount",
            "returnAmount", "return_amount",
            "netSales", "net_sales");

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public InventorySalesReportResponse getInventorySalesReport(
            InventorySalesReportFilter filter,
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

        InventorySalesReportPageDto details = new InventorySalesReportPageDto();
        details.setData(fetchPage(report.sql(), report.params(), filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        InventorySalesReportResponse response = new InventorySalesReportResponse();
        response.setSummary(fetchSummary(report.sql(), report.params()));
        response.setProductSalesDetails(details);
        response.setGeneratedBy(report.user().getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
    }


    // Runs the count and the KPI totals now, so bad filters, missing access
    // and the Excel row limit fail before any of the file has been sent.
    @Override
    public ReportExport exportInventorySalesReport(
            InventorySalesReportFilter filter,
            String format,
            UserDetails user) {

        ExportFormat exportFormat = ExportFormat.parse(format);
        ReportQuery report = prepareReport(filter, user);

        long totalRecords = fetchTotalRecords(report.sql(), report.params());

        if (exportFormat == ExportFormat.XLSX && totalRecords > EXCEL_ROW_LIMIT) {
            throw new IllegalArgumentException(
                    "Too many rows for Excel (" + totalRecords + "). Narrow the filters or download CSV.");
        }

        InventorySalesReportSummaryDto summary = fetchSummary(report.sql(), report.params());
        String generatedBy = report.user().getFullName();
        LocalDateTime generatedOn = LocalDateTime.now();
        String pharmacies = pharmacyNames(report.pharmacyIds());

        boolean singleDay = report.fromDate().equals(report.toDate());
        String period = singleDay
                ? report.fromDate().format(DISPLAY_DATE)
                : report.fromDate().format(DISPLAY_DATE) + " to " + report.toDate().format(DISPLAY_DATE);
        // inventory-sales_2026-10-08, inventory-sales_2026-10-01_to_2026-10-08
        String fileName = "inventory-sales_"
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

                    writer.title("Inventory Sales Report");
                    writer.line("Pharmacy", pharmacies);
                    writer.line("Period", period);
                    writer.line("Generated By", generatedBy);
                    writer.line("Generated On", generatedOn);
                    writer.blank();

                    writer.line("Total Sale Amount", summary.getTotalSaleAmount());
                    writer.line("Total Return Amount", summary.getTotalReturnAmount());
                    writer.line("Total Net Sales", summary.getTotalNetSales());
                    writer.line("Total Net Sale Quantity", summary.getTotalNetSaleQuantity());
                    writer.blank();

                    writer.header(EXPORT_HEADERS);

                    for (long offset = 0; offset < totalRecords; offset += EXPORT_BATCH_SIZE) {

                        List<InventorySalesReportRowDto> batch =
                                fetchPage(report.sql(), report.params(), filter, EXPORT_BATCH_SIZE, offset);

                        for (InventorySalesReportRowDto row : batch) {
                            writer.row(Arrays.asList(
                                    row.getPharmacyName(), row.getProductName(),
                                    row.getSaleQuantity(), row.getReturnQuantity(), row.getNetSaleQuantity(),
                                    row.getSaleAmount(), row.getReturnAmount(), row.getNetSales()));
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


    private ReportQuery prepareReport(InventorySalesReportFilter filter, UserDetails user) {

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
     *   sales   - billed quantity / net amount per product, bills in the period
     *   returns - returned quantity / net amount per product, Completed returns
     *             dated in the period (so a return of an older bill still counts
     *             against the period it happened in)
     *   report  - every product that was sold or returned, joined to the product
     *             master, with product-level filters applied once here
     *
     * Bill-level filters (pharmacy, referred by, patient type) are applied in
     * both halves; for returns they are read from the original bill.
     */
    private String buildReportSql(
            InventorySalesReportFilter filter,
            List<String> pharmacyIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        params.put("pharmacyIds", pharmacyIds);
        params.put("fromTs", fromDate.atStartOfDay());
        params.put("toTs", toDate.plusDays(1).atStartOfDay());
        params.put("completedStatus", SalesReturnStatus.COMPLETED.getLabel());

        StringBuilder billFilters = new StringBuilder();

        if (!isEmpty(filter.getDoctorIds())) {
            billFilters.append(" AND b.doctor_id IN (:doctorIds)");
            params.put("doctorIds", filter.getDoctorIds());
        }

        if (!isEmpty(filter.getPatientTypes())) {
            billFilters.append(" AND b.customer_type IN (:patientTypes)");
            params.put("patientTypes", parsePatientTypes(filter.getPatientTypes()));
        }

        StringBuilder productFilters = new StringBuilder();

        if (!isEmpty(filter.getProductIds())) {
            productFilters.append(" AND p.product_id IN (:productIds)");
            params.put("productIds", filter.getProductIds());
        }

        if (!isEmpty(filter.getProductNames())) {
            productFilters.append(" AND LOWER(TRIM(p.product_name)) IN (:productNames)");
            params.put("productNames", filter.getProductNames().stream()
                    .map(n -> n.trim().toLowerCase())
                    .toList());
        }

        if (!isEmpty(filter.getProductCategoryIds())) {
            productFilters.append(" AND p.product_category_id IN (:productCategoryIds)");
            params.put("productCategoryIds", filter.getProductCategoryIds());
        }

        if (!isEmpty(filter.getHsnCodes())) {
            productFilters.append(" AND p.hsn_no IN (:hsnCodes)");
            params.put("hsnCodes", filter.getHsnCodes());
        }

        if (hasText(filter.getHsnSearch())) {
            productFilters.append(" AND p.hsn_no ILIKE :hsnSearch");
            params.put("hsnSearch", escapeLike(filter.getHsnSearch().trim()) + "%");
        }

        // gst_percentage is a text column ("12", "12.00", "12%"), so it is
        // reduced to its digits and compared as a number.
        // The explicit " " before each text block matters: a text block drops
        // its leading indentation, which would glue AND onto the previous
        // fragment (e.g. ":hsnSearchAND").
        if (!isEmpty(filter.getGstPercentages())) {
            productFilters.append(" ").append("""
                     AND CAST(NULLIF(regexp_replace(p.gst_percentage, '[^0-9.]', '', 'g'), '') AS NUMERIC)
                         IN (:gstPercentages)""");
            params.put("gstPercentages", filter.getGstPercentages());
        }

        // A product matches when any of its molecules carries one of the schedules.
        if (!isEmpty(filter.getDrugSchedules())) {
            productFilters.append(" ").append("""
                     AND EXISTS (
                         SELECT 1
                         FROM pharma_product_attribute_drug d
                         JOIN pharma_product_molecule pm ON pm.product_attribute_id = d.product_attribute_id
                         JOIN pharma_molecules_master m ON m.molecule_id = pm.molecule_id
                         WHERE d.product_id = p.product_id
                           AND UPPER(TRIM(m.drug_schedule)) IN (:drugSchedules)
                     )""");
            params.put("drugSchedules", filter.getDrugSchedules().stream()
                    .map(s -> s.trim().toUpperCase())
                    .toList());
        }

        return """
                WITH sales AS (
                    SELECT b.pharmacy_id,
                           bd.product_id,
                           SUM(bd.bill_quantity) AS qty,
                           SUM(bd.net_amount)    AS amount
                    FROM pharma_billing_details bd
                    JOIN pharma_billing b ON b.billing_id = bd.billing_id
                    WHERE b.pharmacy_id IN (:pharmacyIds)
                      AND b.created_at >= :fromTs
                      AND b.created_at <  :toTs
                      %1$s
                    GROUP BY b.pharmacy_id, bd.product_id
                ),
                returns AS (
                    SELECT sr.pharmacy_id,
                           srd.product_id,
                           SUM(srd.sales_return_quantity) AS qty,
                           SUM(srd.net_amount)            AS amount
                    FROM pharma_sales_return_details srd
                    JOIN pharma_sales_return sr ON sr.sales_return_id = srd.sales_return_id
                    JOIN pharma_billing b ON b.billing_id = sr.billing_id
                    WHERE sr.pharmacy_id IN (:pharmacyIds)
                      AND sr.sales_return_status = :completedStatus
                      AND COALESCE(sr.sales_return_date, sr.created_at) >= :fromTs
                      AND COALESCE(sr.sales_return_date, sr.created_at) <  :toTs
                      %1$s
                    GROUP BY sr.pharmacy_id, srd.product_id
                ),
                report AS (
                    SELECT ids.pharmacy_id,
                           ph.pharmacy_name,
                           p.product_id,
                           p.product_name,
                           COALESCE(s.qty, 0)                         AS sale_quantity,
                           COALESCE(r.qty, 0)                         AS return_quantity,
                           COALESCE(s.qty, 0) - COALESCE(r.qty, 0)       AS net_sale_quantity,
                           COALESCE(s.amount, 0)                      AS sale_amount,
                           COALESCE(r.amount, 0)                      AS return_amount,
                           COALESCE(s.amount, 0) - COALESCE(r.amount, 0) AS net_sales
                    FROM (SELECT pharmacy_id, product_id FROM sales
                          UNION
                          SELECT pharmacy_id, product_id FROM returns) ids
                    JOIN pharma_product_details p ON p.product_id = ids.product_id
                    JOIN pharma_pharmacy_details ph ON ph.pharmacy_id = ids.pharmacy_id
                    LEFT JOIN sales s
                           ON s.pharmacy_id = ids.pharmacy_id AND s.product_id = ids.product_id
                    LEFT JOIN returns r
                           ON r.pharmacy_id = ids.pharmacy_id AND r.product_id = ids.product_id
                    WHERE 1 = 1
                      %2$s
                )
                """.formatted(billFilters, productFilters);
    }


    private InventorySalesReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COALESCE(SUM(sale_amount), 0),
                       COALESCE(SUM(return_amount), 0),
                       COALESCE(SUM(net_sales), 0),
                       COALESCE(SUM(net_sale_quantity), 0)
                FROM report
                """, params).getSingleResult();

        InventorySalesReportSummaryDto summary = new InventorySalesReportSummaryDto();
        summary.setTotalSaleAmount(toAmount(row[0]));
        summary.setTotalReturnAmount(toAmount(row[1]));
        summary.setTotalNetSales(toAmount(row[2]));
        summary.setTotalNetSaleQuantity(toLong(row[3]));

        return summary;
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<InventorySalesReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            InventorySalesReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "netSales", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // pharmacy_id + product_id as tie-breakers keep page boundaries stable
        // when several rows share the same sort value.
        Query query = nativeQuery(reportSql + """
                SELECT pharmacy_name, product_name,
                       sale_quantity, return_quantity, net_sale_quantity,
                       sale_amount, return_amount, net_sales
                FROM report
                ORDER BY %s %s NULLS LAST, pharmacy_id, product_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<InventorySalesReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            InventorySalesReportRowDto dto = new InventorySalesReportRowDto();
            dto.setPharmacyName((String) r[0]);
            dto.setProductName((String) r[1]);
            dto.setSaleQuantity(toLong(r[2]));
            dto.setReturnQuantity(toLong(r[3]));
            dto.setNetSaleQuantity(toLong(r[4]));
            dto.setSaleAmount(toAmount(r[5]));
            dto.setReturnAmount(toAmount(r[6]));
            dto.setNetSales(toAmount(r[7]));
            rows.add(dto);
        }

        return rows;
    }


    private Query nativeQuery(String sql, Map<String, Object> params) {

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }


    private List<String> parsePatientTypes(List<String> values) {

        List<String> types = new ArrayList<>();

        for (String value : values) {
            CustomerType type;
            try {
                type = CustomerType.valueOf(value.trim().toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException e) {
                type = null;
            }

            if (type == null || !REPORT_PATIENT_TYPES.contains(type)) {
                throw new IllegalArgumentException("Invalid patientType: " + value
                        + ". Allowed: " + REPORT_PATIENT_TYPES);
            }
            types.add(type.name());
        }

        return types;
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
