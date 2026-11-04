package tiameds.pharmabackend.service.impl.auditReports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportFilter;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportPageDto;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportResponse;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportRowDto;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionReportSummaryDto;
import tiameds.pharmabackend.dto.auditReports.UserWiseCollectionUserRowDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.PaymentMode;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.auditReports.UserWiseCollectionReportService;

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
public class UserWiseCollectionReportServiceImpl implements UserWiseCollectionReportService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // Modes that bring money in. CREDIT defers payment, so it is never a collection.
    private static final Set<PaymentMode> COLLECTION_MODES = EnumSet.of(
            PaymentMode.CASH,
            PaymentMode.UPI,
            PaymentMode.CARD);

    // Customer/Patient types offered on the report filter.
    private static final Set<CustomerType> REPORT_CUSTOMER_TYPES = EnumSet.of(
            CustomerType.WALK_IN,
            CustomerType.OP_PATIENT,
            CustomerType.IP_PATIENT,
            CustomerType.DAYCARE);

    // Whitelists of sortable columns: the client sends the key, never SQL.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("collectionDate", "collected_ts"),
            Map.entry("receiptNo", "payment_id"),
            Map.entry("billNo", "bill_no"),
            Map.entry("userName", "user_name"),
            Map.entry("customerType", "customer_type"),
            Map.entry("paymentMode", "payment_mode"),
            Map.entry("collectedAmount", "collected_amount"));

    private static final Map<String, String> SUMMARY_SORT_COLUMNS = Map.ofEntries(
            Map.entry("userName", "user_name"),
            Map.entry("noOfCollections", "no_of_collections"),
            Map.entry("cash", "cash"),
            Map.entry("upi", "upi"),
            Map.entry("card", "card"),
            Map.entry("totalCollection", "total_collection"));

    @PersistenceContext
    private EntityManager entityManager;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final LocationContextResolver locationContextResolver;


    @Override
    public UserWiseCollectionReportResponse getUserWiseCollectionReport(
            UserWiseCollectionReportFilter filter,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate fromDate = filter.getFromDate() != null ? filter.getFromDate() : LocalDate.now();
        LocalDate toDate = filter.getToDate() != null ? filter.getToDate() : fromDate;

        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate cannot be before fromDate.");
        }

        List<String> pharmacyIds = resolvePharmacyIds(filter.getPharmacyIds(), persistentUser);

        int size = filter.getSize() == null || filter.getSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(filter.getSize(), MAX_PAGE_SIZE);
        int page = filter.getPage() == null || filter.getPage() < 0 ? 0 : filter.getPage();
        long offset = (long) page * size;

        Map<String, Object> params = new HashMap<>();
        String reportSql = buildReportSql(filter, pharmacyIds, fromDate, toDate, params);

        long totalRecords = fetchTotalRecords(reportSql, params);
        List<UserWiseCollectionReportRowDto> rows = fetchPage(reportSql, params, filter, size, offset);

        UserWiseCollectionReportPageDto details = new UserWiseCollectionReportPageDto();
        details.setData(rows);
        details.setDisplayedTotal(rows.stream()
                .map(UserWiseCollectionReportRowDto::getCollectedAmount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add));
        details.setPage(page);
        details.setSize(size);
        details.setTotalRecords(totalRecords);
        details.setTotalPages((int) ((totalRecords + size - 1) / size));

        UserWiseCollectionReportResponse response = new UserWiseCollectionReportResponse();
        response.setSummary(fetchSummary(reportSql, params));
        response.setUserWiseSummary(fetchUserWiseSummary(reportSql, params, filter));
        response.setCollectionTransactions(details);
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
     * Builds the report as a CTE; callers wrap it with SELECT ... FROM report.
     *
     *   report - one row per billing receipt (pharma_billing_payment) in a
     *            Cash / UPI / Card mode with a positive received amount,
     *            dated by when it was recorded, with every filter applied
     *            once here. The collecting user is the receipt's created_by.
     *
     * Receipts have no number of their own, so the receipt number is derived
     * from the payment id (RCP-00101).
     */
    private String buildReportSql(
            UserWiseCollectionReportFilter filter,
            List<String> pharmacyIds,
            LocalDate fromDate,
            LocalDate toDate,
            Map<String, Object> params) {

        params.put("pharmacyIds", pharmacyIds);
        params.put("fromTs", fromDate.atStartOfDay());
        params.put("toTs", toDate.plusDays(1).atStartOfDay());

        List<String> paymentModes = isEmpty(filter.getPaymentModes())
                ? COLLECTION_MODES.stream().map(Enum::name).toList()
                : parsePaymentModes(filter.getPaymentModes());
        params.put("paymentModes", paymentModes);

        StringBuilder reportFilters = new StringBuilder();

        if (!isEmpty(filter.getUserIds())) {
            reportFilters.append(" AND bp.created_by IN (:userIds)");
            params.put("userIds", filter.getUserIds().stream()
                    .map(String::trim)
                    .distinct()
                    .toList());
        }

        if (!isEmpty(filter.getCustomerTypes())) {
            reportFilters.append(" AND b.customer_type IN (:customerTypes)");
            params.put("customerTypes", parseCustomerTypes(filter.getCustomerTypes()));
        }

        return """
                WITH report AS (
                    SELECT bp.payment_id,
                           'RCP-' || LPAD(CAST(bp.payment_id AS TEXT), 5, '0') AS receipt_no,
                           bp.created_at                                       AS collected_ts,
                           b.billing_id,
                           b.bill_no,
                           ph.pharmacy_name,
                           bp.created_by                                       AS user_id,
                           COALESCE(u.full_name, bp.created_by)                AS user_name,
                           b.customer_type,
                           bp.payment_mode,
                           bp.transaction_id,
                           bp.received_amount                                  AS collected_amount
                    FROM pharma_billing_payment bp
                    JOIN pharma_billing b ON b.billing_id = bp.billing_id
                    JOIN pharma_pharmacy_details ph ON ph.pharmacy_id = b.pharmacy_id
                    LEFT JOIN pharma_user_details u ON u.user_id = bp.created_by
                    WHERE b.pharmacy_id IN (:pharmacyIds)
                      AND bp.payment_mode IN (:paymentModes)
                      AND bp.received_amount > 0
                      AND bp.created_at >= :fromTs
                      AND bp.created_at <  :toTs
                      %s
                )
                """.formatted(reportFilters);
    }


    private UserWiseCollectionReportSummaryDto fetchSummary(String reportSql, Map<String, Object> params) {

        Object[] row = (Object[]) nativeQuery(reportSql + """
                SELECT COUNT(*),
                       COALESCE(SUM(collected_amount), 0),
                       COALESCE(SUM(collected_amount) FILTER (WHERE payment_mode = 'CASH'), 0),
                       COALESCE(SUM(collected_amount) FILTER (WHERE payment_mode = 'UPI'), 0),
                       COALESCE(SUM(collected_amount) FILTER (WHERE payment_mode = 'CARD'), 0)
                FROM report
                """, params).getSingleResult();

        UserWiseCollectionReportSummaryDto summary = new UserWiseCollectionReportSummaryDto();
        summary.setTotalCollections(toLong(row[0]));
        summary.setTotalCollection(toAmount(row[1]));
        summary.setTotalCashCollection(toAmount(row[2]));
        summary.setTotalUpiCollection(toAmount(row[3]));
        summary.setTotalCardCollection(toAmount(row[4]));
        summary.setCashPercentage(
                percentage(summary.getTotalCashCollection(), summary.getTotalCollection()));
        summary.setUpiPercentage(
                percentage(summary.getTotalUpiCollection(), summary.getTotalCollection()));
        summary.setCardPercentage(
                percentage(summary.getTotalCardCollection(), summary.getTotalCollection()));

        return summary;
    }


    // One row per collecting user, every matching user (not paged).
    @SuppressWarnings("unchecked")
    private List<UserWiseCollectionUserRowDto> fetchUserWiseSummary(
            String reportSql,
            Map<String, Object> params,
            UserWiseCollectionReportFilter filter) {

        String sortColumn = SUMMARY_SORT_COLUMNS.getOrDefault(
                hasText(filter.getSummarySortBy()) ? filter.getSummarySortBy() : "totalCollection", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid summarySortBy: " + filter.getSummarySortBy()
                    + ". Allowed: " + String.join(", ", SUMMARY_SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSummarySortDir()) ? "ASC" : "DESC";

        List<Object[]> results = nativeQuery(reportSql + """
                SELECT user_id, user_name, no_of_collections, cash, upi, card, total_collection
                FROM (
                    SELECT user_id,
                           MAX(user_name)                                                  AS user_name,
                           COUNT(*)                                                        AS no_of_collections,
                           COALESCE(SUM(collected_amount) FILTER (WHERE payment_mode = 'CASH'), 0) AS cash,
                           COALESCE(SUM(collected_amount) FILTER (WHERE payment_mode = 'UPI'), 0)  AS upi,
                           COALESCE(SUM(collected_amount) FILTER (WHERE payment_mode = 'CARD'), 0) AS card,
                           COALESCE(SUM(collected_amount), 0)                              AS total_collection
                    FROM report
                    GROUP BY user_id
                ) users
                ORDER BY %s %s NULLS LAST, user_name, user_id
                """.formatted(sortColumn, sortDir), params).getResultList();

        List<UserWiseCollectionUserRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            UserWiseCollectionUserRowDto dto = new UserWiseCollectionUserRowDto();
            dto.setUserId((String) r[0]);
            dto.setUserName((String) r[1]);
            dto.setNoOfCollections(toLong(r[2]));
            dto.setCash(toAmount(r[3]));
            dto.setUpi(toAmount(r[4]));
            dto.setCard(toAmount(r[5]));
            dto.setTotalCollection(toAmount(r[6]));
            rows.add(dto);
        }

        return rows;
    }


    private long fetchTotalRecords(String reportSql, Map<String, Object> params) {

        return toLong(nativeQuery(reportSql + "SELECT COUNT(*) FROM report", params)
                .getSingleResult());
    }


    @SuppressWarnings("unchecked")
    private List<UserWiseCollectionReportRowDto> fetchPage(
            String reportSql,
            Map<String, Object> params,
            UserWiseCollectionReportFilter filter,
            int size,
            long offset) {

        String sortColumn = SORT_COLUMNS.getOrDefault(
                hasText(filter.getSortBy()) ? filter.getSortBy() : "collectionDate", null);

        if (sortColumn == null) {
            throw new IllegalArgumentException("Invalid sortBy: " + filter.getSortBy()
                    + ". Allowed: " + String.join(", ", SORT_COLUMNS.keySet()));
        }

        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "ASC" : "DESC";

        // collected_ts + payment_id as tie-breakers keep page boundaries
        // stable when several rows share the sort value.
        Query query = nativeQuery(reportSql + """
                SELECT collected_ts, payment_id, receipt_no,
                       billing_id, bill_no, pharmacy_name,
                       user_id, user_name, customer_type,
                       payment_mode, transaction_id, collected_amount
                FROM report
                ORDER BY %s %s NULLS LAST, collected_ts DESC, payment_id DESC
                LIMIT :limit OFFSET :offset
                """.formatted(sortColumn, sortDir), params);

        query.setParameter("limit", size);
        query.setParameter("offset", offset);

        List<Object[]> results = query.getResultList();
        List<UserWiseCollectionReportRowDto> rows = new ArrayList<>(results.size());

        for (Object[] r : results) {
            UserWiseCollectionReportRowDto dto = new UserWiseCollectionReportRowDto();
            dto.setCollectionDate(toLocalDate(r[0]));
            dto.setPaymentId(toLong(r[1]));
            dto.setReceiptNo((String) r[2]);
            dto.setBillingId(toLong(r[3]));
            dto.setBillNo((String) r[4]);
            dto.setPharmacyName((String) r[5]);
            dto.setUserId((String) r[6]);
            dto.setUserName((String) r[7]);
            dto.setCustomerType((String) r[8]);
            dto.setPaymentMode((String) r[9]);
            dto.setTransactionId((String) r[10]);
            dto.setCollectedAmount(toAmount(r[11]));
            rows.add(dto);
        }

        return rows;
    }


    private Query nativeQuery(String sql, Map<String, Object> params) {

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return query;
    }


    private List<String> parsePaymentModes(List<String> values) {

        List<String> modes = new ArrayList<>();

        for (String value : values) {
            PaymentMode mode;
            try {
                mode = PaymentMode.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                mode = null;
            }

            if (mode == null || !COLLECTION_MODES.contains(mode)) {
                throw new IllegalArgumentException("Invalid paymentMode: " + value
                        + ". Allowed: " + COLLECTION_MODES);
            }
            modes.add(mode.name());
        }

        return modes;
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


    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
