package tiameds.pharmabackend.service.impl.financialReport.dayClosingSummary;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportResponse;
import tiameds.pharmabackend.dto.financialReport.common.ReportPageDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.BillCountSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.BillCountSummaryRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.BillingByUserKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.BillingByUserRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.CurrentBillDetailsKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.CurrentBillDetailsRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.PatientTypeBillingKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.PatientTypeBillingKpiDto.PatientTypeTotal;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.PatientTypeBillingRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.ReceiptByUserKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.ReceiptByUserRowDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.SalesReturnDetailsKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.SalesReturnDetailsRowDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingPayment;
import tiameds.pharmabackend.entity.billing.CustomerManagement;
import tiameds.pharmabackend.entity.billing.SalesReturn;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.PaymentMode;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.financialReport.dayClosingSummary.DayClosingSummaryService;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.DateRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Day closing summary reports, each covering a single business day.
 * <p>
 * Bill count summary: bills raised on the day, and how many of them were
 * fully paid / still pending (IP) by the end of that day. A bill is settled by
 * payments plus completed returns, so a returned credit bill is no longer
 * pending. Payments and returns recorded after the day do not change a past
 * day's figures.
 * <p>
 * Billing by patient type: bill count and amount of the day's bills for IP,
 * OP, Walk-In and Day Care, plus a total of those rows.
 * <p>
 * Billing by user / receipt by user: the day's bills per user who raised
 * them, and the day's payments per user who recorded them.
 * <p>
 * Current bill details / sales return details: the day's bills and completed
 * returns, one row each, paged.
 * <p>
 * The user filter applies to whoever did the transaction the report is about:
 * the bill's creator, the payment's creator or the return's creator.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DayClosingSummaryServiceImpl implements DayClosingSummaryService {

    // Row order and labels of the billing by patient type report.
    private static final List<CustomerType> PATIENT_TYPE_ORDER = List.of(
            CustomerType.IP_PATIENT,
            CustomerType.OP_PATIENT,
            CustomerType.WALK_IN,
            CustomerType.DAYCARE);

    private static final Map<CustomerType, PatientTypeLabel> PATIENT_TYPE_LABELS = new EnumMap<>(Map.of(
            CustomerType.IP_PATIENT, new PatientTypeLabel("IP", "IP"),
            CustomerType.OP_PATIENT, new PatientTypeLabel("OP", "OP"),
            CustomerType.WALK_IN, new PatientTypeLabel("WALK_IN", "Walk-In"),
            CustomerType.DAYCARE, new PatientTypeLabel("DAY_CARE", "Day Care")));

    private static final PatientTypeTotal EMPTY_TOTAL = new PatientTypeTotal(0, BigDecimal.ZERO);

    private final FinancialReportSupport support;
    private final UserDetailsRepository userDetailsRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public FinancialReportResponse<BillCountSummaryKpiDto, BillCountSummaryRowDto> getBillCountSummary(
            FinancialReportFilter filter,
            UserDetails user) {

        Scope scope = resolveScope(filter, user);

        long totalBills = countBills(scope, BillStatus.ANY);
        long fullyPaidBills = countBills(scope, BillStatus.FULLY_PAID);
        long pendingIpBills = countBills(scope, BillStatus.PENDING_IP);

        BillCountSummaryKpiDto kpi = new BillCountSummaryKpiDto();
        kpi.setTotalBills(totalBills);
        kpi.setFullyPaidBills(fullyPaidBills);
        kpi.setPendingIpBills(pendingIpBills);

        List<BillCountSummaryRowDto> rows = List.of(
                new BillCountSummaryRowDto("TOTAL_BILLS", "Total Bills", totalBills),
                new BillCountSummaryRowDto("FULLY_PAID_BILLS", "Fully Paid Bills", fullyPaidBills),
                new BillCountSummaryRowDto("PENDING_IP_BILLS", "Partially Paid / Pending IP Bills",
                        pendingIpBills));

        return response(user, kpi, singlePage(rows));
    }


    @Override
    public FinancialReportResponse<PatientTypeBillingKpiDto, PatientTypeBillingRowDto> getBillingByPatientType(
            FinancialReportFilter filter,
            UserDetails user) {

        Scope scope = resolveScope(filter, user);

        Map<CustomerType, PatientTypeTotal> totals = sumBillsByPatientType(scope);

        // Every patient type when none is selected, otherwise just the selected ones.
        List<CustomerType> shown = PATIENT_TYPE_ORDER.stream()
                .filter(type -> scope.patientTypes().isEmpty() || scope.patientTypes().contains(type))
                .toList();

        List<PatientTypeBillingRowDto> rows = new ArrayList<>();
        long totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CustomerType type : shown) {
            PatientTypeTotal total = totals.getOrDefault(type, EMPTY_TOTAL);
            PatientTypeLabel label = PATIENT_TYPE_LABELS.get(type);
            rows.add(new PatientTypeBillingRowDto(
                    label.code(), label.text(), total.getBillCount(), total.getTotalAmount()));
            totalCount += total.getBillCount();
            totalAmount = totalAmount.add(total.getTotalAmount());
        }

        rows.add(new PatientTypeBillingRowDto("TOTAL", "Total", totalCount, totalAmount));

        PatientTypeBillingKpiDto kpi = new PatientTypeBillingKpiDto();
        kpi.setIp(totals.getOrDefault(CustomerType.IP_PATIENT, EMPTY_TOTAL));
        kpi.setOp(totals.getOrDefault(CustomerType.OP_PATIENT, EMPTY_TOTAL));
        kpi.setWalkIn(totals.getOrDefault(CustomerType.WALK_IN, EMPTY_TOTAL));
        kpi.setDayCare(totals.getOrDefault(CustomerType.DAYCARE, EMPTY_TOTAL));

        return response(user, kpi, singlePage(rows));
    }


    @Override
    public FinancialReportResponse<BillingByUserKpiDto, BillingByUserRowDto> getBillingByUser(
            FinancialReportFilter filter,
            UserDetails user) {

        Scope scope = resolveScope(filter, user);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<Billing> bill = query.from(Billing.class);
        Join<Billing, PharmacyDetails> pharmacy = bill.join("pharmacy");
        Path<String> createdBy = bill.get("createdBy");

        List<Predicate> predicates = eligibleBill(cb, query, bill, scope, true, createdBy);
        predicates.addAll(support.within(cb, bill.get("createdAt"), scope.range()));

        List<UserTotal> totals = sumPerUser(cb, query, pharmacy, createdBy,
                cb.count(bill), cb.coalesce(cb.sum(support.billAmount(cb, bill)), BigDecimal.ZERO), predicates);

        List<BillingByUserRowDto> rows = new ArrayList<>();
        long totalBills = 0;
        BigDecimal totalSales = BigDecimal.ZERO;

        for (UserTotal total : totals) {
            rows.add(new BillingByUserRowDto("USER", total.pharmacyId(), total.pharmacyName(),
                    total.userId(), total.userName(), total.count(), total.amount()));
            totalBills += total.count();
            totalSales = totalSales.add(total.amount());
        }

        rows.add(new BillingByUserRowDto("TOTAL", null, null, null, "Total", totalBills, totalSales));

        BillingByUserKpiDto kpi = new BillingByUserKpiDto();
        kpi.setTotalBills(totalBills);
        kpi.setTotalSales(totalSales);

        return response(user, kpi, singlePage(rows));
    }


    @Override
    public FinancialReportResponse<ReceiptByUserKpiDto, ReceiptByUserRowDto> getReceiptByUser(
            FinancialReportFilter filter,
            UserDetails user) {

        Scope scope = resolveScope(filter, user);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<BillingPayment> payment = query.from(BillingPayment.class);
        Join<BillingPayment, Billing> bill = payment.join("billing");
        Join<Billing, PharmacyDetails> pharmacy = bill.join("pharmacy");
        Path<String> createdBy = payment.get("createdBy");

        // Real money only: credit entries carry nothing received. The payment
        // mode filter applies to the payment itself.
        List<Predicate> predicates = eligibleBill(cb, query, bill, scope, false, createdBy);
        predicates.addAll(support.within(cb, payment.get("createdAt"), scope.range()));
        predicates.add(cb.notEqual(payment.get("paymentMode"), PaymentMode.CREDIT));

        if (!scope.paymentModes().isEmpty()) {
            predicates.add(payment.get("paymentMode").in(scope.paymentModes()));
        }

        List<UserTotal> totals = sumPerUser(cb, query, pharmacy, createdBy,
                cb.count(payment),
                cb.coalesce(cb.sum(payment.<BigDecimal>get("receivedAmount")), BigDecimal.ZERO),
                predicates);

        List<ReceiptByUserRowDto> rows = new ArrayList<>();
        long totalReceipts = 0;
        BigDecimal totalReceived = BigDecimal.ZERO;

        for (UserTotal total : totals) {
            rows.add(new ReceiptByUserRowDto("USER", total.pharmacyId(), total.pharmacyName(),
                    total.userId(), total.userName(), total.count(), total.amount()));
            totalReceipts += total.count();
            totalReceived = totalReceived.add(total.amount());
        }

        rows.add(new ReceiptByUserRowDto("TOTAL", null, null, null, "Total", totalReceipts, totalReceived));

        ReceiptByUserKpiDto kpi = new ReceiptByUserKpiDto();
        kpi.setTotalReceipts(totalReceipts);
        kpi.setTotalAmountReceived(totalReceived);

        return response(user, kpi, singlePage(rows));
    }


    @Override
    public FinancialReportResponse<CurrentBillDetailsKpiDto, CurrentBillDetailsRowDto> getCurrentBillDetails(
            FinancialReportFilter filter,
            UserDetails user) {

        Scope scope = resolveScope(filter, user);
        int page = support.resolvePage(filter);
        int size = support.resolveSize(filter);

        CurrentBillDetailsKpiDto kpi = currentBillTotals(scope);
        support.checkPrintLimit(filter, kpi.getTotalBills());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<Billing> bill = query.from(Billing.class);
        Join<Billing, PharmacyDetails> pharmacy = bill.join("pharmacy");
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);

        query.multiselect(
                        pharmacy.get("pharmacyId").alias("pharmacyId"),
                        pharmacy.get("pharmacyName").alias("pharmacyName"),
                        bill.get("billingId").alias("billingId"),
                        bill.get("billNo").alias("billNo"),
                        customer.get("customerName").alias("customerName"),
                        bill.get("customerType").alias("customerType"),
                        bill.get("createdAt").alias("billedAt"),
                        support.billAmount(cb, bill).alias("totalAmount"),
                        receivedByDayEnd(cb, query, bill, scope).alias("received"),
                        returnedByDayEnd(cb, query, bill, scope).alias("returned"))
                .where(dayBills(cb, query, bill, scope).toArray(Predicate[]::new))
                .orderBy(
                        cb.asc(pharmacy.get("pharmacyName")),
                        cb.asc(bill.get("createdAt")),
                        cb.asc(bill.get("billingId")));

        List<CurrentBillDetailsRowDto> rows = entityManager.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(row -> {
                    CustomerType type = row.get("customerType", CustomerType.class);
                    BigDecimal amount = orZero(row.get("totalAmount", BigDecimal.class));
                    BigDecimal received = orZero(row.get("received", BigDecimal.class));
                    BigDecimal returned = orZero(row.get("returned", BigDecimal.class));

                    return new CurrentBillDetailsRowDto(
                            row.get("pharmacyId", String.class),
                            row.get("pharmacyName", String.class),
                            row.get("billingId", Long.class),
                            row.get("billNo", String.class),
                            row.get("customerName", String.class),
                            patientTypeLabel(type),
                            row.get("billedAt", LocalDateTime.class),
                            amount,
                            received,
                            returned,
                            type == CustomerType.IP_PATIENT
                                    ? pending(amount, received, returned)
                                    : BigDecimal.ZERO);
                })
                .toList();

        return response(user, kpi, page(rows, page, size, kpi.getTotalBills()));
    }


    @Override
    public FinancialReportResponse<SalesReturnDetailsKpiDto, SalesReturnDetailsRowDto> getSalesReturnDetails(
            FinancialReportFilter filter,
            UserDetails user) {

        Scope scope = resolveScope(filter, user);
        int page = support.resolvePage(filter);
        int size = support.resolveSize(filter);

        SalesReturnDetailsKpiDto kpi = salesReturnTotals(scope);
        support.checkPrintLimit(filter, kpi.getTotalReturns());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<SalesReturn> salesReturn = query.from(SalesReturn.class);
        Join<SalesReturn, Billing> bill = salesReturn.join("billing");
        Join<Billing, PharmacyDetails> pharmacy = bill.join("pharmacy");
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);

        query.multiselect(
                        pharmacy.get("pharmacyId").alias("pharmacyId"),
                        pharmacy.get("pharmacyName").alias("pharmacyName"),
                        salesReturn.get("salesReturnId").alias("salesReturnId"),
                        salesReturn.get("salesReturnNo").alias("salesReturnNo"),
                        bill.get("billNo").alias("billNo"),
                        customer.get("customerName").alias("customerName"),
                        salesReturn.get("salesReturnDate").alias("returnedAt"),
                        salesReturn.get("totalNetAmount").alias("returnAmount"))
                .where(dayReturns(cb, query, salesReturn, bill, scope).toArray(Predicate[]::new))
                .orderBy(
                        cb.asc(pharmacy.get("pharmacyName")),
                        cb.asc(salesReturn.get("salesReturnDate")),
                        cb.asc(salesReturn.get("salesReturnId")));

        List<SalesReturnDetailsRowDto> rows = entityManager.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(row -> new SalesReturnDetailsRowDto(
                        row.get("pharmacyId", String.class),
                        row.get("pharmacyName", String.class),
                        row.get("salesReturnId", Long.class),
                        row.get("salesReturnNo", String.class),
                        row.get("billNo", String.class),
                        row.get("customerName", String.class),
                        row.get("returnedAt", LocalDateTime.class),
                        orZero(row.get("returnAmount", BigDecimal.class))))
                .toList();

        return response(user, kpi, page(rows, page, size, kpi.getTotalReturns()));
    }


    // Bill count and amount of the day's eligible bills, per patient type.
    // Only the four types on the report are counted.
    private Map<CustomerType, PatientTypeTotal> sumBillsByPatientType(Scope scope) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<Billing> bill = query.from(Billing.class);

        List<Predicate> predicates = dayBills(cb, query, bill, scope);
        predicates.add(bill.get("customerType").in(PATIENT_TYPE_ORDER));

        query.multiselect(
                        bill.get("customerType").alias("type"),
                        cb.count(bill).alias("billCount"),
                        cb.coalesce(cb.sum(support.billAmount(cb, bill)), BigDecimal.ZERO).alias("amount"))
                .where(predicates.toArray(Predicate[]::new))
                .groupBy(bill.get("customerType"));

        Map<CustomerType, PatientTypeTotal> totals = new EnumMap<>(CustomerType.class);

        for (Tuple row : entityManager.createQuery(query).getResultList()) {
            totals.put(
                    row.get("type", CustomerType.class),
                    new PatientTypeTotal(row.get("billCount", Long.class), row.get("amount", BigDecimal.class)));
        }

        return totals;
    }


    // Count and amount per pharmacy and user, ordered by pharmacy then user
    // name. Names are looked up afterwards; a user id with no user record is
    // shown as is.
    private List<UserTotal> sumPerUser(
            CriteriaBuilder cb,
            CriteriaQuery<Tuple> query,
            Join<?, PharmacyDetails> pharmacy,
            Path<String> userColumn,
            Expression<Long> count,
            Expression<BigDecimal> amount,
            List<Predicate> predicates) {

        query.multiselect(
                        pharmacy.get("pharmacyId").alias("pharmacyId"),
                        pharmacy.get("pharmacyName").alias("pharmacyName"),
                        userColumn.alias("userId"),
                        count.alias("count"),
                        amount.alias("amount"))
                .where(predicates.toArray(Predicate[]::new))
                .groupBy(pharmacy.get("pharmacyId"), pharmacy.get("pharmacyName"), userColumn);

        List<Tuple> results = entityManager.createQuery(query).getResultList();

        List<String> userIds = results.stream()
                .map(row -> row.get("userId", String.class))
                .filter(FinancialReportSupport::hasText)
                .distinct()
                .toList();

        Map<String, String> names = new HashMap<>();

        for (UserDetails user : userDetailsRepository.findAllById(userIds)) {
            names.put(user.getUserId(), FinancialReportSupport.generatedBy(user));
        }

        return results.stream()
                .map(row -> {
                    String userId = row.get("userId", String.class);
                    return new UserTotal(
                            row.get("pharmacyId", String.class),
                            row.get("pharmacyName", String.class),
                            userId,
                            names.getOrDefault(userId, userId),
                            row.get("count", Long.class),
                            orZero(row.get("amount", BigDecimal.class)));
                })
                .sorted(Comparator
                        .comparing(UserTotal::pharmacyName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UserTotal::userName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }


    // KPIs of the current bill details over all of the day's matching bills:
    // amount billed, received and returned by the end of the day, and still
    // due on IP. One small row per bill, summed here so each IP bill's pending
    // is floored at zero on its own.
    private CurrentBillDetailsKpiDto currentBillTotals(Scope scope) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<Billing> bill = query.from(Billing.class);

        query.multiselect(
                        bill.get("customerType").alias("type"),
                        support.billAmount(cb, bill).alias("amount"),
                        receivedByDayEnd(cb, query, bill, scope).alias("received"),
                        returnedByDayEnd(cb, query, bill, scope).alias("returned"))
                .where(dayBills(cb, query, bill, scope).toArray(Predicate[]::new));

        long count = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalReturned = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;

        for (Tuple row : entityManager.createQuery(query).getResultList()) {
            BigDecimal amount = orZero(row.get("amount", BigDecimal.class));
            BigDecimal received = orZero(row.get("received", BigDecimal.class));
            BigDecimal returned = orZero(row.get("returned", BigDecimal.class));

            count++;
            totalAmount = totalAmount.add(amount);
            totalReceived = totalReceived.add(received);
            totalReturned = totalReturned.add(returned);

            if (row.get("type", CustomerType.class) == CustomerType.IP_PATIENT) {
                totalPending = totalPending.add(pending(amount, received, returned));
            }
        }

        CurrentBillDetailsKpiDto kpi = new CurrentBillDetailsKpiDto();
        kpi.setTotalBills(count);
        kpi.setTotalAmount(totalAmount);
        kpi.setTotalAmountReceived(totalReceived);
        kpi.setTotalReturnedAmount(totalReturned);
        kpi.setTotalPendingAmount(totalPending);

        return kpi;
    }


    // Still due on a bill after payments and returns, never below zero.
    private static BigDecimal pending(BigDecimal amount, BigDecimal received, BigDecimal returned) {

        return amount.subtract(received).subtract(returned).max(BigDecimal.ZERO);
    }


    // Count and amount of all of the day's matching returns.
    private SalesReturnDetailsKpiDto salesReturnTotals(Scope scope) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<SalesReturn> salesReturn = query.from(SalesReturn.class);
        Join<SalesReturn, Billing> bill = salesReturn.join("billing");

        query.multiselect(
                        cb.count(salesReturn).alias("count"),
                        cb.coalesce(cb.sum(salesReturn.<BigDecimal>get("totalNetAmount")), BigDecimal.ZERO)
                                .alias("amount"))
                .where(dayReturns(cb, query, salesReturn, bill, scope).toArray(Predicate[]::new));

        Tuple totals = entityManager.createQuery(query).getSingleResult();

        SalesReturnDetailsKpiDto kpi = new SalesReturnDetailsKpiDto();
        kpi.setTotalReturns(totals.get("count", Long.class));
        kpi.setTotalReturnAmount(orZero(totals.get("amount", BigDecimal.class)));

        return kpi;
    }


    // Eligible bills raised on the day, narrowed to the given settlement
    // status as of the end of the day.
    private long countBills(Scope scope, BillStatus status) {

        if (status == BillStatus.PENDING_IP
                && !scope.patientTypes().isEmpty()
                && !scope.patientTypes().contains(CustomerType.IP_PATIENT)) {
            return 0;
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<Billing> bill = query.from(Billing.class);

        List<Predicate> predicates = dayBills(cb, query, bill, scope);

        Expression<BigDecimal> amount = support.billAmount(cb, bill);

        // Settled = received + completed returns, both by the end of the day.
        switch (status) {
            case FULLY_PAID -> predicates.add(cb.lessThanOrEqualTo(amount, cb.sum(
                    receivedByDayEnd(cb, query, bill, scope),
                    returnedByDayEnd(cb, query, bill, scope))));
            case PENDING_IP -> {
                predicates.add(cb.equal(bill.get("customerType"), CustomerType.IP_PATIENT));
                predicates.add(cb.greaterThan(amount, cb.sum(
                        receivedByDayEnd(cb, query, bill, scope),
                        returnedByDayEnd(cb, query, bill, scope))));
            }
            case ANY -> {
            }
        }

        query.select(cb.count(bill))
                .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    // Received on the bill up to the end of the day, zero when nothing was.
    private Expression<BigDecimal> receivedByDayEnd(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            From<?, Billing> bill,
            Scope scope) {

        Subquery<BigDecimal> received = query.subquery(BigDecimal.class);
        Root<BillingPayment> payment = received.from(BillingPayment.class);

        received.select(cb.coalesce(cb.sum(payment.<BigDecimal>get("receivedAmount")), BigDecimal.ZERO))
                .where(
                        cb.equal(payment.get("billing"), bill),
                        cb.lessThan(payment.get("createdAt"), scope.range().endExclusive()));

        return received;
    }


    // Completed returns against the bill up to the end of the day, zero when none.
    private Expression<BigDecimal> returnedByDayEnd(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            From<?, Billing> bill,
            Scope scope) {

        return support.returnedAmount(cb, query, bill, scope.range().endExclusive());
    }


    // Eligible bills raised on the day; the user filter matches the bill's creator.
    private List<Predicate> dayBills(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            From<?, Billing> bill,
            Scope scope) {

        List<Predicate> predicates = eligibleBill(cb, query, bill, scope, true, bill.get("createdBy"));
        predicates.addAll(support.within(cb, bill.get("createdAt"), scope.range()));

        return predicates;
    }


    // Completed returns dated on the day, against eligible bills; the user
    // filter matches the return's creator.
    private List<Predicate> dayReturns(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            Root<SalesReturn> salesReturn,
            From<?, Billing> bill,
            Scope scope) {

        List<Predicate> predicates = eligibleBill(cb, query, bill, scope, true, salesReturn.get("createdBy"));
        predicates.add(cb.equal(salesReturn.get("salesReturnStatus"), SalesReturnStatus.COMPLETED));
        predicates.addAll(support.within(cb, salesReturn.get("salesReturnDate"), scope.range()));

        return predicates;
    }


    // Pharmacy, patient type, payment mode and user filters; the date is left
    // to the caller. With byPaymentMode, a bill matches the payment mode filter
    // when any of its payments used a selected mode. The user filter is
    // matched against userColumn.
    private List<Predicate> eligibleBill(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            From<?, Billing> bill,
            Scope scope,
            boolean byPaymentMode,
            Expression<String> userColumn) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(bill.get("pharmacy").get("pharmacyId").in(scope.pharmacyIds()));

        if (!scope.patientTypes().isEmpty()) {
            predicates.add(bill.get("customerType").in(scope.patientTypes()));
        }

        if (byPaymentMode && !scope.paymentModes().isEmpty()) {
            predicates.add(support.paidWithAnyOf(cb, query, bill, scope.paymentModes()));
        }

        if (!scope.userIds().isEmpty()) {
            predicates.add(userColumn.in(scope.userIds()));
        }

        return predicates;
    }


    private Scope resolveScope(FinancialReportFilter filter, UserDetails user) {

        return new Scope(
                support.resolvePharmacyIds(filter, user),
                support.resolveBusinessDay(filter),
                support.resolvePatientTypes(filter),
                support.resolvePaymentModes(filter),
                FinancialReportSupport.trimmed(filter.getUserIds()));
    }


    private static String patientTypeLabel(CustomerType type) {

        if (type == null) {
            return null;
        }

        PatientTypeLabel label = PATIENT_TYPE_LABELS.get(type);

        return label == null ? type.name() : label.text();
    }


    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }


    private static <K, T> FinancialReportResponse<K, T> response(UserDetails user, K kpi, ReportPageDto<T> page) {

        return new FinancialReportResponse<>(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                kpi,
                page);
    }


    // Fixed-row reports: every row on one page.
    private static <T> ReportPageDto<T> singlePage(List<T> rows) {
        return new ReportPageDto<>(rows, 0, rows.size(), rows.size(), 1);
    }


    private static <T> ReportPageDto<T> page(List<T> rows, int page, int size, long totalElements) {
        return new ReportPageDto<>(rows, page, size, totalElements, (int) ((totalElements + size - 1) / size));
    }


    private enum BillStatus { ANY, FULLY_PAID, PENDING_IP }


    private record PatientTypeLabel(String code, String text) {
    }


    private record UserTotal(
            String pharmacyId,
            String pharmacyName,
            String userId,
            String userName,
            long count,
            BigDecimal amount) {
    }


    private record Scope(
            List<String> pharmacyIds,
            DateRange range,
            List<CustomerType> patientTypes,
            List<PaymentMode> paymentModes,
            List<String> userIds) {
    }
}
