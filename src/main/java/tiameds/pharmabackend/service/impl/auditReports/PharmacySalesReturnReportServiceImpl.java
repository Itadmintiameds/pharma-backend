package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportFilter;
import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportPageDto;
import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportResponse;
import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportRowDto;
import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnReportSummaryDto;
import tiameds.pharmabackend.dto.auditReports.PharmacySalesReturnSettlementRowDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.auditReports.PharmacySalesReturnReportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PharmacySalesReturnReportServiceImpl implements PharmacySalesReturnReportService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // Customer/Patient types offered on the report filter.
    private static final Set<CustomerType> REPORT_CUSTOMER_TYPES = EnumSet.of(
            CustomerType.WALK_IN,
            CustomerType.OP_PATIENT,
            CustomerType.IP_PATIENT,
            CustomerType.DAYCARE);

    // Bills whose unpaid balance a return is first adjusted against; every
    // other return is refunded in full. These are also the bills whose
    // op_ip_number is an IP Number.
    private static final List<String> ADJUSTABLE_CUSTOMER_TYPES = List.of(
            CustomerType.IP_PATIENT.name(),
            CustomerType.DAYCARE.name());

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("returnDate", "return_ts"),
            Map.entry("salesReturnNo", "sales_return_no"),
            Map.entry("billNo", "bill_no"),
            Map.entry("pharmacyName", "pharmacy_name"),
            Map.entry("customerType", "customer_type"),
            Map.entry("productName", "product_name"),
            Map.entry("batchNumber", "batch_number"),
            Map.entry("expiryDate", "expiry_date"),
            Map.entry("returnQuantity", "return_quantity"),
            Map.entry("returnReason", "return_reason"),
            Map.entry("sellingAmount", "selling_amount"),
            Map.entry("discountAmount", "discount_amount"),
            Map.entry("taxableAmount", "taxable_amount"),
            Map.entry("gstAmount", "gst_amount"),
            Map.entry("lineReturnAmount", "line_return_amount"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public PharmacySalesReturnReportResponse getPharmacySalesReturnReport(
            PharmacySalesReturnReportFilter filter,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate fromDate = filter.getFromDate() != null ? filter.getFromDate() : LocalDate.now();
        LocalDate toDate = filter.getToDate() != null ? filter.getToDate() : fromDate;

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate cannot be before fromDate.");
        }

        requireRange(filter.getMinRefundAmount(), filter.getMaxRefundAmount(),
                "maxRefundAmount cannot be less than minRefundAmount.");
        requireRange(filter.getMinOutstandingAdjustment(), filter.getMaxOutstandingAdjustment(),
                "maxOutstandingAdjustment cannot be less than minOutstandingAdjustment.");

        List<String> pharmacyIds = resolvePharmacyIds(filter.getPharmacyIds(), persistentUser);

        int size = filter.getSize() == null || filter.getSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(filter.getSize(), MAX_PAGE_SIZE);
        int page = filter.getPage() == null || filter.getPage() < 0 ? 0 : filter.getPage();
        long offset = (long) page * size;

        Map<String, Object> params = new HashMap<>();
        String reportSql = buildReportSql(filter, pharmacyIds, fromDate, toDate, params);

        long totalRecords = fetchTotalRecords(reportSql, params);

        PharmacySalesReturnReportPageDto details = new PharmacySalesReturnReportPageDto();
        details.setData(fetchPage(reportSql, params, filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        PharmacySalesReturnReportResponse response = new PharmacySalesReturnReportResponse();
        response.setSummary(fetchSummary(reportSql, params));
        response.setSalesReturnTransactions(details);
        response.setSettlementSummary(fetchSettlementSummary(reportSql, params));
        response.setStatus(SalesReturnStatus.COMPLETED.getLabel());
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setGeneratedBy(persistentUser.getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
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
     *   period_bills  - bills with a Completed return dated in the period
     *   return_totals - every Completed return of those bills (also ones
     *                   outside the period) with its return amount, the sum
     *                   of its lines' net amounts
     *   bill_due      - what each bill still owed: rounded net amount less
     *                   every payment received against it
     *   settlement    - per return, the part adjusted against that balance.
     *                   For IP Patient / Daycare bills the returns consume the
     *                   balance in return-date order, so a return's share is
     *                   LEAST(running total, due) - LEAST(previous running
     *                   total, due). The rest of the return is refunded.
     *   report        - one row per returned product + batch line in the
     *                   period, with every filter applied once here. A
     *                   return's adjustment is split over its lines by net
     *                   amount, so totals stay consistent when product-level
     *                   filters keep only some of a return's lines.
     *
     * Discount is not stored on the return line, so it is reversed at the
     * original bill line's per-unit discount for that product + batch.
     * Gross amount is the taxable amount, as on the bill.
     */
    private String buildReportSql(
            PharmacySalesReturnReportFilter filter,
            List<String> pharmacyIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        params.put("pharmacyIds", pharmacyIds);
        params.put("fromTs", fromDate.atStartOfDay());
        params.put("toTs", toDate.plusDays(1).atStartOfDay());
        params.put("completedStatus", SalesReturnStatus.COMPLETED.getLabel());
        params.put("adjustableTypes", ADJUSTABLE_CUSTOMER_TYPES);

        StringBuilder reportFilters = new StringBuilder();

        if (!isEmpty(filter.getCustomerTypes())) {
            reportFilters.append(" AND b.customer_type IN (:customerTypes)");
            params.put("customerTypes", parseCustomerTypes(filter.getCustomerTypes()));
        }

        appendSearch(reportFilters, params, "sr.sales_return_no", "salesReturnNo", filter.getSalesReturnNo());
        appendSearch(reportFilters, params, "b.bill_no", "billNo", filter.getBillNo());
        appendSearch(reportFilters, params, "c.customer_name", "customerName", filter.getCustomerName());
        appendSearch(reportFilters, params, "c.customer_phone_no", "mobileNumber", filter.getMobileNumber());
        appendSearch(reportFilters, params, "c.patient_number", "patientNumber", filter.getPatientNumber());
        appendSearch(reportFilters, params, "p.product_name", "productName", filter.getProductName());
        appendSearch(reportFilters, params, "bt.batch_number", "batchNumber", filter.getBatchNumber());

        // OP and IP numbers share the bill's op_ip_number column; the customer
        // type says which one it is.
        if (hasText(filter.getOpNumber())) {
            reportFilters.append(" AND b.customer_type = :opType AND b.op_ip_number ILIKE :opNumber");
            params.put("opType", CustomerType.OP_PATIENT.name());
            params.put("opNumber", "%" + escapeLike(filter.getOpNumber().trim()) + "%");
        }

        if (hasText(filter.getIpNumber())) {
            reportFilters.append(" AND b.customer_type IN (:adjustableTypes) AND b.op_ip_number ILIKE :ipNumber");
            params.put("ipNumber", "%" + escapeLike(filter.getIpNumber().trim()) + "%");
        }

        if (!isEmpty(filter.getProductIds())) {
            reportFilters.append(" AND p.product_id IN (:productIds)");
            params.put("productIds", filter.getProductIds());
        }

        if (!isEmpty(filter.getProductCategoryIds())) {
            reportFilters.append(" AND p.product_category_id IN (:productCategoryIds)");
            params.put("productCategoryIds", filter.getProductCategoryIds());
        }

        if (!isEmpty(filter.getReturnReasons())) {
            reportFilters.append(" AND LOWER(TRIM(srd.sales_return_reason)) IN (:returnReasons)");
            params.put("returnReasons", filter.getReturnReasons().stream()
                    .map(r -> r.trim().toLowerCase())
                    .toList());
        }

        // The historical rate is derived from the line's own amounts, so it is
        // compared at one decimal place to absorb paise rounding (4.99 -> 5.0).
        if (!isEmpty(filter.getGstPercentages())) {
            reportFilters.append(" ").append("""
                     AND ROUND(CAST(srd.gst_amount * 100 / NULLIF(srd.gross_amount, 0) AS NUMERIC), 1)
                         IN (:gstPercentages)""");
            params.put("gstPercentages", filter.getGstPercentages().stream()
                    .map(g -> g.setScale(1, RoundingMode.HALF_UP))
                    .toList());
        }

        // Refund and outstanding adjustment ranges apply to the whole return.
        if (filter.getMinRefundAmount() != null) {
            reportFilters.append(" AND s.doc_amount - s.doc_adjustment >= :minRefundAmount");
            params.put("minRefundAmount", filter.getMinRefundAmount());
        }

        if (filter.getMaxRefundAmount() != null) {
            reportFilters.append(" AND s.doc_amount - s.doc_adjustment <= :maxRefundAmount");
            params.put("maxRefundAmount", filter.getMaxRefundAmount());
        }

        if (filter.getMinOutstandingAdjustment() != null) {
            reportFilters.append(" AND s.doc_adjustment >= :minOutstandingAdjustment");
            params.put("minOutstandingAdjustment", filter.getMinOutstandingAdjustment());
        }

        if (filter.getMaxOutstandingAdjustment() != null) {
            reportFilters.append(" AND s.doc_adjustment <= :maxOutstandingAdjustment");
            params.put("maxOutstandingAdjustment", filter.getMaxOutstandingAdjustment());
        }

        return """
                WITH period_bills AS (
                    SELECT DISTINCT sr.billing_id
                    FROM pharma_sales_return sr
                    WHERE sr.pharmacy_id IN (:pharmacyIds)
                      AND sr.sales_return_status = :completedStatus
                      AND COALESCE(sr.sales_return_date, sr.created_at) >= :fromTs
                      AND COALESCE(sr.sales_return_date, sr.created_at) <  :toTs
                ),
                return_totals AS (
                    SELECT sr.sales_return_id,
                           sr.billing_id,
                           COALESCE(sr.sales_return_date, sr.created_at) AS return_ts,
                           COALESCE(SUM(srd.net_amount), 0)              AS doc_amount
                    FROM pharma_sales_return sr
                    JOIN period_bills pb ON pb.billing_id = sr.billing_id
                    LEFT JOIN pharma_sales_return_details srd ON srd.sales_return_id = sr.sales_return_id
                    WHERE sr.sales_return_status = :completedStatus
                    GROUP BY sr.sales_return_id, sr.billing_id,
                             COALESCE(sr.sales_return_date, sr.created_at)
                ),
                bill_due AS (
                    SELECT b.billing_id,
                           b.customer_type,
                           GREATEST(
                               COALESCE(b.total_net_amount_after_round_off, b.total_net_amount, 0)
                               - COALESCE((SELECT SUM(bp.received_amount)
                                           FROM pharma_billing_payment bp
                                           WHERE bp.billing_id = b.billing_id), 0),
                               0) AS due
                    FROM pharma_billing b
                    JOIN period_bills pb ON pb.billing_id = b.billing_id
                ),
                settlement AS (
                    SELECT rt.sales_return_id,
                           rt.return_ts,
                           rt.doc_amount,
                           CASE WHEN bd.customer_type IN (:adjustableTypes)
                                THEN LEAST(SUM(rt.doc_amount) OVER w, bd.due)
                                   - LEAST(SUM(rt.doc_amount) OVER w - rt.doc_amount, bd.due)
                                ELSE 0
                           END AS doc_adjustment
                    FROM return_totals rt
                    JOIN bill_due bd ON bd.billing_id = rt.billing_id
                    WINDOW w AS (PARTITION BY rt.billing_id ORDER BY rt.return_ts, rt.sales_return_id)
                ),
                report AS (
                    SELECT srd.sales_return_detail_id,
                           sr.sales_return_id,
                           sr.sales_return_no,
                           s.return_ts,
                           b.bill_no,
                           ph.pharmacy_name,
                           b.customer_type,
                           p.product_name,
                           bt.batch_number,
                           bt.expiry_date,
                           COALESCE(srd.sales_return_quantity, 0)                         AS return_quantity,
                           srd.sales_return_reason                                        AS return_reason,
                           COALESCE(srd.gross_amount, 0)
                               + COALESCE(disc.per_unit * srd.sales_return_quantity, 0)   AS selling_amount,
                           COALESCE(disc.per_unit * srd.sales_return_quantity, 0)         AS discount_amount,
                           COALESCE(srd.gross_amount, 0)                                  AS taxable_amount,
                           COALESCE(srd.gst_amount, 0)                                    AS gst_amount,
                           COALESCE(srd.net_amount, 0)                                    AS line_return_amount,
                           CASE WHEN s.doc_amount <> 0
                                THEN s.doc_adjustment * COALESCE(srd.net_amount, 0) / s.doc_amount
                                ELSE 0
                           END                                                            AS outstanding_adjustment
                    FROM settlement s
                    JOIN pharma_sales_return sr ON sr.sales_return_id = s.sales_return_id
                    JOIN pharma_sales_return_details srd ON srd.sales_return_id = sr.sales_return_id
                    JOIN pharma_billing b ON b.billing_id = sr.billing_id
                    JOIN pharma_pharmacy_details ph ON ph.pharmacy_id = sr.pharmacy_id
                    JOIN pharma_product_details p ON p.product_id = srd.product_id
                    LEFT JOIN pharma_batch_details bt ON bt.batch_id = srd.batch_id
                    LEFT JOIN pharma_customer_management c ON c.customer_id = b.customer_id
                    LEFT JOIN LATERAL (
                        SELECT SUM(bl.discount_amount) / NULLIF(SUM(bl.bill_quantity), 0) AS per_unit
                        FROM pharma_billing_details bl
                        WHERE bl.billing_id = sr.billing_id
                          AND bl.product_id = srd.product_id
                          AND bl.batch_id = srd.batch_id
                    ) disc ON TRUE
                    WHERE sr.pharmacy_id IN (:pharmacyIds)
                      AND s.return_ts >= :fromTs
                      AND s.return_ts <  :toTs
                      %s
                )
                """.formatted(reportFilters);
    }


    private PharmacySalesReturnReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COUNT(DISTINCT sales_return_id),
                       COUNT(*),
                       COALESCE(SUM(return_quantity), 0),
                       COALESCE(SUM(selling_amount), 0),
                       COALESCE(SUM(discount_amount), 0),
                       COALESCE(SUM(taxable_amount), 0),
                       COALESCE(SUM(gst_amount), 0),
                       COALESCE(SUM(line_return_amount), 0),
                       COALESCE(SUM(outstanding_adjustment), 0),
                       COALESCE(SUM(line_return_amount - outstanding_adjustment), 0)
                FROM report
                """, params).getSingleResult();

        PharmacySalesReturnReportSummaryDto summary = new PharmacySalesReturnReportSummaryDto();
        summary.setTotalSalesReturnTransactions(toLong(row[0]));
        summary.setTotalReturnLines(toLong(row[1]));
        summary.setTotalReturnQuantity(toLong(row[2]));
        summary.setTotalHistoricalAmount(toAmount(row[3]));
        summary.setTotalDiscountReversed(toAmount(row[4]));
        summary.setTotalTaxableAmountReversed(toAmount(row[5]));
        summary.setTotalGstReversed(toAmount(row[6]));
        summary.setTotalReturnAmount(toAmount(row[7]));
        summary.setTotalOutstandingAdjustment(toAmount(row[8]));
        summary.setTotalRefundAmount(toAmount(row[9]));
        summary.setOutstandingAdjustmentPercentage(
                percentage(summary.getTotalOutstandingAdjustment(), summary.getTotalReturnAmount()));
        summary.setRefundPercentage(
                percentage(summary.getTotalRefundAmount(), summary.getTotalReturnAmount()));

        return summary;
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<PharmacySalesReturnReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            PharmacySalesReturnReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "returnDate", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "desc".equalsIgnoreCase(filter.getSortDir()) ? "DESC" : "ASC";

        // return_ts + sales_return_id + sales_return_detail_id as tie-breakers
        // keep page boundaries stable when several rows share the sort value.
        Query query = nativeQuery(reportSql + """
                SELECT return_ts, sales_return_id, sales_return_no, bill_no,
                       pharmacy_name, customer_type,
                       product_name, batch_number, expiry_date,
                       return_quantity, return_reason,
                       selling_amount, discount_amount, taxable_amount, gst_amount,
                       line_return_amount
                FROM report
                ORDER BY %s %s NULLS LAST, return_ts, sales_return_id, sales_return_detail_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<PharmacySalesReturnReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            PharmacySalesReturnReportRowDto dto = new PharmacySalesReturnReportRowDto();
            dto.setReturnDate(toLocalDate(r[0]));
            dto.setSalesReturnId(toLong(r[1]));
            dto.setSalesReturnNo((String) r[2]);
            dto.setBillNo((String) r[3]);
            dto.setPharmacyName((String) r[4]);
            dto.setCustomerType((String) r[5]);
            dto.setProductName((String) r[6]);
            dto.setBatchNumber((String) r[7]);
            dto.setExpiryDate(toLocalDate(r[8]));
            dto.setReturnQuantity(toLong(r[9]));
            dto.setReturnReason((String) r[10]);
            dto.setSellingAmount(toAmount(r[11]));
            dto.setDiscountAmount(toAmount(r[12]));
            dto.setTaxableAmount(toAmount(r[13]));
            dto.setGstAmount(toAmount(r[14]));
            dto.setLineReturnAmount(toAmount(r[15]));
            rows.add(dto);
        }

        return rows;
    }


    // One row per matching return, in return order, summed over its matching lines.
    @SuppressWarnings("unchecked")
    private List<PharmacySalesReturnSettlementRowDto> fetchSettlementSummary(
            String reportSql,
            Map<String, Object> params) {

        List<Object[]> results = nativeQuery(reportSql + """
                SELECT sales_return_id, sales_return_no, customer_type,
                       SUM(line_return_amount),
                       SUM(outstanding_adjustment),
                       SUM(line_return_amount - outstanding_adjustment)
                FROM report
                GROUP BY sales_return_id, sales_return_no, customer_type, return_ts
                ORDER BY return_ts, sales_return_id
                """, params).getResultList();

        List<PharmacySalesReturnSettlementRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            PharmacySalesReturnSettlementRowDto dto = new PharmacySalesReturnSettlementRowDto();
            dto.setSalesReturnId(toLong(r[0]));
            dto.setSalesReturnNo((String) r[1]);
            dto.setCustomerType((String) r[2]);
            dto.setReturnAmount(toAmount(r[3]));
            dto.setOutstandingAdjustment(toAmount(r[4]));
            dto.setRefundAmount(toAmount(r[5]));
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


    private static void requireRange(BigDecimal min, BigDecimal max, String message) {

        if (min != null && max != null && max.compareTo(min) < 0) {
            throw new IllegalArgumentException(message);
        }
    }


    private static BigDecimal percentage(BigDecimal part, BigDecimal total) {

        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
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
