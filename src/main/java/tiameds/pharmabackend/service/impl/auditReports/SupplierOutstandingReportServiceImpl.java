package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportFilter;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportPageDto;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportResponse;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportRowDto;
import tiameds.pharmabackend.dto.auditReports.SupplierOutstandingReportSummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.enums.PurchaseReturnStatus;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.auditReports.SupplierOutstandingReportService;

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
public class SupplierOutstandingReportServiceImpl implements SupplierOutstandingReportService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    // Purchases bought on credit; only these carry a payable to the supplier.
    private static final String PAYMENT_TYPE_CREDIT = "CREDIT";

    private static final String STATUS_UNPAID = "Unpaid";
    private static final String STATUS_PARTIALLY_SETTLED = "Partially Settled";
    private static final String STATUS_FULLY_SETTLED = "Fully Settled";

    // Outstanding Status dropdown value -> the statuses it keeps.
    private static final Map<String, List<String>> OUTSTANDING_STATUSES = Map.of(
            "OUTSTANDING_ONLY", List.of(STATUS_UNPAID, STATUS_PARTIALLY_SETTLED),
            "UNPAID", List.of(STATUS_UNPAID),
            "PARTIALLY_SETTLED", List.of(STATUS_PARTIALLY_SETTLED),
            "FULLY_SETTLED", List.of(STATUS_FULLY_SETTLED));

    // Whitelist of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("supplierName", "supplier_name"),
            Map.entry("invoiceNumber", "invoice_no"),
            Map.entry("grnNumber", "grn_no"),
            Map.entry("invoiceDate", "invoice_date"),
            Map.entry("dueDate", "due_date"),
            Map.entry("invoiceAmount", "invoice_amount"),
            Map.entry("totalPaid", "total_paid"),
            Map.entry("purchaseReturnAdjusted", "return_adjusted"),
            Map.entry("currentOutstanding", "current_outstanding"),
            Map.entry("overdueDays", "overdue_days"),
            Map.entry("supplierCredit", "supplier_credit"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public SupplierOutstandingReportResponse getSupplierOutstandingReport(
            SupplierOutstandingReportFilter filter,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate fromDate = filter.getFromDate() != null ? filter.getFromDate() : LocalDate.now();
        LocalDate toDate = filter.getToDate() != null ? filter.getToDate() : fromDate;

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate cannot be before fromDate.");
        }

        if (filter.getDueFromDate() != null && filter.getDueToDate() != null
                && filter.getDueToDate().isBefore(filter.getDueFromDate())) {
            throw new IllegalArgumentException("dueToDate cannot be before dueFromDate.");
        }

        if (filter.getMinOutstanding() != null && filter.getMaxOutstanding() != null
                && filter.getMaxOutstanding().compareTo(filter.getMinOutstanding()) < 0) {
            throw new IllegalArgumentException("maxOutstanding cannot be less than minOutstanding.");
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

        LocalDate today = LocalDate.now();

        int size = filter.getSize() == null || filter.getSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(filter.getSize(), MAX_PAGE_SIZE);
        int page = filter.getPage() == null || filter.getPage() < 0 ? 0 : filter.getPage();
        long offset = (long) page * size;

        Map<String, Object> params = new HashMap<>();
        String reportSql = buildReportSql(filter, pharmacyIds, warehouseIds, fromDate, toDate, today, params);

        long totalRecords = fetchTotalRecords(reportSql, params);

        SupplierOutstandingReportPageDto details = new SupplierOutstandingReportPageDto();
        details.setData(totalRecords == 0
                ? List.of()
                : fetchPage(reportSql, params, filter, size, offset));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        SupplierOutstandingReportResponse response = new SupplierOutstandingReportResponse();
        response.setSummary(fetchSummary(reportSql, params));
        response.setOutstandingInvoices(details);
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setOutstandingAsOf(today);
        response.setGeneratedBy(persistentUser.getFullName());
        response.setGeneratedOn(LocalDateTime.now());

        return response;
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
     *              or warehouse and dated in the period, narrowed by the
     *              invoice-level filters
     *   paid     - supplier payments per invoice
     *   returned - confirmed purchase returns per invoice
     *   calc     - balance = invoice amount - paid - returned; a positive
     *              balance is still outstanding, a negative one is credit the
     *              supplier owes back
     *   derived  - outstanding, supplier credit, overdue days and status
     *   report   - derived narrowed by the filters on those figures
     *
     * Same rules as the Supplier Payment KPI cards: invoice amount is the
     * purchase net total and the due date is invoice date + credit days.
     */
    private String buildReportSql(
            SupplierOutstandingReportFilter filter,
            List<String> pharmacyIds,
            List<String> warehouseIds,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDate today,
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
        params.put("fromTs", Timestamp.valueOf(fromDate.atStartOfDay()));
        params.put("toTs", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        params.put("today", Date.valueOf(today));
        params.put("statusUnpaid", STATUS_UNPAID);
        params.put("statusPartial", STATUS_PARTIALLY_SETTLED);
        params.put("statusSettled", STATUS_FULLY_SETTLED);

        // Each fragment starts with a space so it never glues onto the
        // previous one (a text block would drop that leading space).
        StringBuilder invoiceFilters = new StringBuilder();

        if (!isEmpty(filter.getSupplierIds())) {
            invoiceFilters.append(" AND pu.supplier_id IN (:supplierIds)");
            params.put("supplierIds", filter.getSupplierIds());
        }

        if (hasText(filter.getSupplierSearch())) {
            invoiceFilters.append(" AND sm.supplier_name ILIKE :supplierSearch");
            params.put("supplierSearch", "%" + escapeLike(filter.getSupplierSearch().trim()) + "%");
        }

        if (hasText(filter.getInvoiceNumber())) {
            invoiceFilters.append(" AND pu.invoice_no ILIKE :invoiceNumber");
            params.put("invoiceNumber", "%" + escapeLike(filter.getInvoiceNumber().trim()) + "%");
        }

        if (hasText(filter.getGrnNumber())) {
            invoiceFilters.append(" AND pu.grn_no ILIKE :grnNumber");
            params.put("grnNumber", "%" + escapeLike(filter.getGrnNumber().trim()) + "%");
        }

        StringBuilder reportFilters = new StringBuilder();

        if (filter.getDueFromDate() != null) {
            reportFilters.append(" AND due_date >= :dueFrom");
            params.put("dueFrom", Date.valueOf(filter.getDueFromDate()));
        }

        if (filter.getDueToDate() != null) {
            reportFilters.append(" AND due_date <= :dueTo");
            params.put("dueTo", Date.valueOf(filter.getDueToDate()));
        }

        List<String> statuses = parseOutstandingStatus(filter.getOutstandingStatus());

        if (statuses != null) {
            reportFilters.append(" AND outstanding_status IN (:statuses)");
            params.put("statuses", statuses);
        }

        if (filter.getMinOutstanding() != null) {
            reportFilters.append(" AND current_outstanding >= :minOutstanding");
            params.put("minOutstanding", filter.getMinOutstanding());
        }

        if (filter.getMaxOutstanding() != null) {
            reportFilters.append(" AND current_outstanding <= :maxOutstanding");
            params.put("maxOutstanding", filter.getMaxOutstanding());
        }

        String supplierCredit = hasText(filter.getSupplierCredit())
                ? filter.getSupplierCredit().trim().toUpperCase().replace(' ', '_').replace('-', '_')
                : "ALL";

        switch (supplierCredit) {
            case "ALL" -> { }
            case "CREDIT" -> reportFilters.append(" AND supplier_credit > 0");
            case "WITHOUT_CREDIT" -> reportFilters.append(" AND supplier_credit = 0");
            default -> throw new IllegalArgumentException("Invalid supplierCredit: "
                    + filter.getSupplierCredit() + ". Allowed: ALL, CREDIT, WITHOUT_CREDIT");
        }

        return """
                WITH invoices AS (
                    SELECT pu.purchase_id,
                           COALESCE(wh.warehouse_name, ph.pharmacy_name) AS location_name,
                           sm.supplier_name,
                           pu.invoice_no,
                           pu.grn_no,
                           CAST(COALESCE(pu.invoice_date, pu.created_at) AS DATE) AS invoice_date,
                           CAST(COALESCE(pu.invoice_date, pu.created_at) AS DATE)
                               + CAST(COALESCE(pu.credit_days, 0) AS INTEGER) AS due_date,
                           COALESCE(pu.total_net_amount, 0) AS invoice_amount
                    FROM pharma_purchase pu
                    LEFT JOIN pharma_supplier_master sm ON sm.supplier_id = pu.supplier_id
                    LEFT JOIN pharma_pharmacy_details ph ON ph.pharmacy_id = pu.pharmacy_id
                    LEFT JOIN pharma_warehouse wh ON wh.warehouse_id = pu.warehouse_id
                    WHERE %3$s
                      AND UPPER(pu.payment_type) = :creditType
                      AND COALESCE(pu.invoice_date, pu.created_at) >= :fromTs
                      AND COALESCE(pu.invoice_date, pu.created_at) <  :toTs
                      %1$s
                ),
                paid AS (
                    SELECT sp.purchase_id,
                           CAST(SUM(sp.payment_amount) AS NUMERIC) AS amount
                    FROM pharma_supplier_payment sp
                    WHERE sp.purchase_id IN (SELECT purchase_id FROM invoices)
                    GROUP BY sp.purchase_id
                ),
                returned AS (
                    SELECT pr.purchase_id,
                           SUM(COALESCE(pr.total_net_amount, 0)) AS amount
                    FROM pharma_purchase_return pr
                    WHERE pr.purchase_id IN (SELECT purchase_id FROM invoices)
                      AND pr.status = :confirmedStatus
                    GROUP BY pr.purchase_id
                ),
                calc AS (
                    SELECT i.*,
                           COALESCE(p.amount, 0) AS total_paid,
                           COALESCE(r.amount, 0) AS return_adjusted,
                           i.invoice_amount - COALESCE(p.amount, 0) - COALESCE(r.amount, 0) AS balance
                    FROM invoices i
                    LEFT JOIN paid p ON p.purchase_id = i.purchase_id
                    LEFT JOIN returned r ON r.purchase_id = i.purchase_id
                ),
                derived AS (
                    SELECT calc.*,
                           GREATEST(balance, 0) AS current_outstanding,
                           GREATEST(-balance, 0) AS supplier_credit,
                           CASE WHEN balance > 0 AND due_date < CAST(:today AS DATE)
                                THEN CAST(:today AS DATE) - due_date
                           END AS overdue_days,
                           CASE WHEN balance <= 0 THEN CAST(:statusSettled AS TEXT)
                                WHEN total_paid + return_adjusted > 0 THEN CAST(:statusPartial AS TEXT)
                                ELSE CAST(:statusUnpaid AS TEXT)
                           END AS outstanding_status
                    FROM calc
                ),
                report AS (
                    SELECT * FROM derived WHERE 1 = 1 %2$s
                )
                """.formatted(invoiceFilters, reportFilters, locationScope);
    }


    // null = no restriction (ALL).
    private static List<String> parseOutstandingStatus(String value) {

        if (!hasText(value)) {
            return null;
        }

        String key = value.trim().toUpperCase().replace(' ', '_').replace('-', '_');

        if ("ALL".equals(key)) {
            return null;
        }

        List<String> statuses = OUTSTANDING_STATUSES.get(key);

        if (statuses == null) {
            throw new IllegalArgumentException("Invalid outstandingStatus: " + value
                    + ". Allowed: ALL, " + String.join(", ", new TreeSet<>(OUTSTANDING_STATUSES.keySet())));
        }

        return statuses;
    }


    private SupplierOutstandingReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COALESCE(SUM(invoice_amount), 0),
                       COUNT(*) FILTER (WHERE current_outstanding > 0),
                       COALESCE(SUM(current_outstanding), 0),
                       COALESCE(SUM(current_outstanding) FILTER (WHERE overdue_days IS NOT NULL), 0),
                       COUNT(*) FILTER (WHERE overdue_days IS NOT NULL),
                       COALESCE(SUM(supplier_credit), 0),
                       COALESCE(SUM(total_paid), 0),
                       COALESCE(SUM(return_adjusted), 0)
                FROM report
                """, params).getSingleResult();

        BigDecimal invoiceAmount = toAmount(row[0]);
        BigDecimal payable = toAmount(row[2]);

        SupplierOutstandingReportSummaryDto summary = new SupplierOutstandingReportSummaryDto();
        summary.setTotalCreditInvoiceAmount(invoiceAmount);
        summary.setOutstandingInvoiceCount(toLong(row[1]));
        summary.setTotalSupplierPayable(payable);
        summary.setPayableSharePercentage(invoiceAmount.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : payable.multiply(BigDecimal.valueOf(100)).divide(invoiceAmount, 2, RoundingMode.HALF_UP));
        summary.setOverduePayable(toAmount(row[3]));
        summary.setOverdueInvoiceCount(toLong(row[4]));
        summary.setSupplierCredit(toAmount(row[5]));
        summary.setTotalPaid(toAmount(row[6]));
        summary.setTotalPurchaseReturnAdjusted(toAmount(row[7]));

        return summary;
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<SupplierOutstandingReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            SupplierOutstandingReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "currentOutstanding", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // purchase_id as tie-breaker keeps page boundaries stable.
        Query query = nativeQuery(reportSql + """
                SELECT purchase_id, location_name, supplier_name,
                       invoice_no, grn_no, invoice_date, due_date,
                       invoice_amount, total_paid, return_adjusted,
                       current_outstanding, supplier_credit,
                       overdue_days, outstanding_status
                FROM report
                ORDER BY %s %s NULLS LAST, purchase_id
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<SupplierOutstandingReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            SupplierOutstandingReportRowDto dto = new SupplierOutstandingReportRowDto();
            dto.setPurchaseId(toLong(r[0]));
            dto.setLocationName((String) r[1]);
            dto.setSupplierName((String) r[2]);
            dto.setInvoiceNumber((String) r[3]);
            dto.setGrnNumber((String) r[4]);
            dto.setInvoiceDate(toLocalDate(r[5]));
            dto.setDueDate(toLocalDate(r[6]));
            dto.setInvoiceAmount(toAmount(r[7]));
            dto.setTotalPaid(toAmount(r[8]));
            dto.setPurchaseReturnAdjusted(toAmount(r[9]));
            dto.setCurrentOutstanding(toAmount(r[10]));
            dto.setSupplierCredit(toAmount(r[11]));
            dto.setOverdueDays(r[12] == null ? null : toLong(r[12]));
            dto.setOutstandingStatus((String) r[13]);
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
