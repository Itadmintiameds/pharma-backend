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
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.PatientTypeBillingKpiDto;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.PatientTypeBillingKpiDto.PatientTypeTotal;
import tiameds.pharmabackend.dto.financialReport.dayClosingSummary.PatientTypeBillingRowDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingPayment;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.PaymentMode;
import tiameds.pharmabackend.service.financialReport.dayClosingSummary.DayClosingSummaryService;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.DateRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Day closing summary reports, each covering a single business day.
 * <p>
 * Bill count summary: bills raised on the day, and how many of them were
 * fully paid / still pending (IP) by the end of that day. Payments recorded
 * after the day do not change a past day's figures.
 * <p>
 * Billing by patient type: bill count and amount of the day's bills for IP,
 * OP, Walk-In and Day Care, plus a total of those rows.
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

        return new FinancialReportResponse<>(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                kpi,
                new ReportPageDto<>(rows, 0, rows.size(), rows.size(), 1));
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

        return new FinancialReportResponse<>(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                kpi,
                new ReportPageDto<>(rows, 0, rows.size(), rows.size(), 1));
    }


    // Bill count and amount of the day's eligible bills, per patient type.
    // Only the four types on the report are counted.
    private Map<CustomerType, PatientTypeTotal> sumBillsByPatientType(Scope scope) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<Billing> bill = query.from(Billing.class);

        List<Predicate> predicates = eligibleBill(cb, query, bill, scope);
        predicates.addAll(support.within(cb, bill.get("createdAt"), scope.range()));
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

        List<Predicate> predicates = eligibleBill(cb, query, bill, scope);
        predicates.addAll(support.within(cb, bill.get("createdAt"), scope.range()));

        Expression<BigDecimal> amount = support.billAmount(cb, bill);

        switch (status) {
            case FULLY_PAID -> predicates.add(
                    cb.lessThanOrEqualTo(amount, receivedByDayEnd(cb, query, bill, scope)));
            case PENDING_IP -> {
                predicates.add(cb.equal(bill.get("customerType"), CustomerType.IP_PATIENT));
                predicates.add(cb.greaterThan(amount, receivedByDayEnd(cb, query, bill, scope)));
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


    // Pharmacy, patient type, payment mode and user filters. A bill matches
    // the payment mode filter when any of its payments used a selected mode.
    private List<Predicate> eligibleBill(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            From<?, Billing> bill,
            Scope scope) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(bill.get("pharmacy").get("pharmacyId").in(scope.pharmacyIds()));

        if (!scope.patientTypes().isEmpty()) {
            predicates.add(bill.get("customerType").in(scope.patientTypes()));
        }

        if (!scope.paymentModes().isEmpty()) {
            predicates.add(support.paidWithAnyOf(cb, query, bill, scope.paymentModes()));
        }

        if (!scope.userIds().isEmpty()) {
            predicates.add(bill.get("createdBy").in(scope.userIds()));
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


    private enum BillStatus { ANY, FULLY_PAID, PENDING_IP }


    private record PatientTypeLabel(String code, String text) {
    }


    private record Scope(
            List<String> pharmacyIds,
            DateRange range,
            List<CustomerType> patientTypes,
            List<PaymentMode> paymentModes,
            List<String> userIds) {
    }
}
