package tiameds.pharmabackend.service.impl.financialReport.billDueSummary;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.financialReport.billDueSummary.BillDueSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.billDueSummary.BillDueSummaryRowDto;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportResponse;
import tiameds.pharmabackend.dto.financialReport.common.ReportPageDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingPayment;
import tiameds.pharmabackend.entity.billing.CustomerManagement;
import tiameds.pharmabackend.entity.billing.DoctorDetails;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.service.financialReport.billDueSummary.BillDueSummaryService;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.DateRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.hasText;

/**
 * Bill due summary: IP bills only (other bill types are settled at the
 * counter), with what has been received against each and what is pending.
 * Received is the sum of the bill's payments, the same figure the billing
 * service settles against. Completed sales returns also settle a bill: a
 * returned sale is no longer owed, so pending = bill - received - returned.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillDueSummaryServiceImpl implements BillDueSummaryService {

    private static final String PENDING = "PENDING";
    private static final String FULLY_PAID = "FULLY_PAID";

    private final FinancialReportSupport support;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public FinancialReportResponse<BillDueSummaryKpiDto, BillDueSummaryRowDto> getBillDueSummary(
            FinancialReportFilter filter,
            UserDetails user) {

        List<String> pharmacyIds = support.resolvePharmacyIds(filter, user);

        DateRange range = support.resolveDateRange(filter);
        String pendingStatus = resolvePendingStatus(filter);
        int page = support.resolvePage(filter);
        int size = support.resolveSize(filter);

        long totalElements = countBills(pharmacyIds, range, pendingStatus, filter);
        support.checkPrintLimit(filter, totalElements);

        List<BillDueSummaryRowDto> rows = totalElements == 0
                ? List.of()
                : findBills(pharmacyIds, range, pendingStatus, filter, page, size);

        ReportPageDto<BillDueSummaryRowDto> billDetails = new ReportPageDto<>(
                rows,
                page,
                size,
                totalElements,
                (int) ((totalElements + size - 1) / size));

        return new FinancialReportResponse<>(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                buildKpi(pharmacyIds, range, pendingStatus, filter),
                billDetails);
    }


    private List<BillDueSummaryRowDto> findBills(
            List<String> pharmacyIds,
            DateRange range,
            String pendingStatus,
            FinancialReportFilter filter,
            int page,
            int size) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<Billing> bill = query.from(Billing.class);
        Join<Billing, PharmacyDetails> pharmacy = bill.join("pharmacy");
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        Expression<BigDecimal> total = totalAmount(cb, bill);
        Expression<BigDecimal> received = receivedAmount(cb, query, bill);
        Expression<BigDecimal> returned = support.returnedAmount(cb, query, bill, null);

        query.multiselect(
                pharmacy.get("pharmacyId").alias("pharmacyId"),
                pharmacy.get("pharmacyName").alias("pharmacyName"),
                bill.get("billingId").alias("billingId"),
                bill.get("billNo").alias("billNo"),
                bill.get("createdAt").alias("billedDate"),
                customer.get("customerId").alias("customerId"),
                customer.get("customerName").alias("patientName"),
                customer.get("customerPhoneNo").alias("patientMobileNo"),
                customer.get("patientNumber").alias("patientNumber"),
                bill.get("opIpNumber").alias("ipNumber"),
                doctor.get("doctorId").alias("doctorId"),
                doctor.get("doctorName").alias("referredBy"),
                total.alias("totalAmount"),
                received.alias("amountReceived"),
                returned.alias("returnedAmount"));

        query.where(billPredicates(cb, query, bill, customer, doctor, pharmacyIds, range, pendingStatus, filter)
                .toArray(Predicate[]::new));

        // One pharmacy's bills after another, newest first within each.
        query.orderBy(
                cb.asc(pharmacy.get("pharmacyName")),
                cb.asc(pharmacy.get("pharmacyId")),
                cb.desc(bill.get("createdAt")),
                cb.desc(bill.get("billingId")));

        return entityManager.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(this::toRow)
                .toList();
    }


    private long countBills(
            List<String> pharmacyIds,
            DateRange range,
            String pendingStatus,
            FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<Billing> bill = query.from(Billing.class);
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        query.select(cb.count(bill))
                .where(billPredicates(cb, query, bill, customer, doctor, pharmacyIds, range, pendingStatus, filter)
                        .toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    // Totals over every matching bill. Pending is worked out per bill and then
    // added up, so a returned (or overpaid) bill counts as zero instead of
    // cancelling out what another bill still owes. Summed here rather than in
    // SQL, since an aggregate over per-bill subqueries is not portable.
    private BillDueSummaryKpiDto buildKpi(
            List<String> pharmacyIds,
            DateRange range,
            String pendingStatus,
            FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<Billing> bill = query.from(Billing.class);
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        query.multiselect(
                        totalAmount(cb, bill).alias("total"),
                        receivedAmount(cb, query, bill).alias("received"),
                        support.returnedAmount(cb, query, bill, null).alias("returned"))
                .where(billPredicates(cb, query, bill, customer, doctor, pharmacyIds, range, pendingStatus, filter)
                        .toArray(Predicate[]::new));

        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalReturned = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;

        for (Tuple row : entityManager.createQuery(query).getResultList()) {
            BigDecimal total = orZero(row.get("total", BigDecimal.class));
            BigDecimal received = orZero(row.get("received", BigDecimal.class));
            BigDecimal returned = orZero(row.get("returned", BigDecimal.class));

            totalBilled = totalBilled.add(total);
            totalReceived = totalReceived.add(received);
            totalReturned = totalReturned.add(returned);
            totalPending = totalPending.add(pending(total, received, returned));
        }

        BillDueSummaryKpiDto kpi = new BillDueSummaryKpiDto();
        kpi.setTotalIpBillAmount(totalBilled);
        kpi.setTotalAmountReceived(totalReceived);
        kpi.setTotalReturnedAmount(totalReturned);
        kpi.setTotalPendingAmount(totalPending);

        return kpi;
    }


    // What is still owed on a bill: neither paid nor returned.
    private static BigDecimal pending(BigDecimal total, BigDecimal received, BigDecimal returned) {
        return total.subtract(received).subtract(returned).max(BigDecimal.ZERO);
    }


    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }


    private List<Predicate> billPredicates(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            From<?, Billing> bill,
            From<?, CustomerManagement> customer,
            From<?, DoctorDetails> doctor,
            List<String> pharmacyIds,
            DateRange range,
            String pendingStatus,
            FinancialReportFilter filter) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(bill.get("pharmacy").get("pharmacyId").in(pharmacyIds));
        predicates.add(cb.equal(bill.get("customerType"), CustomerType.IP_PATIENT));
        predicates.addAll(support.within(cb, bill.get("createdAt"), range));
        predicates.addAll(support.billSearchPredicates(cb, bill, customer, doctor, filter));

        if (pendingStatus != null) {

            // Settled = paid or returned.
            Expression<BigDecimal> total = totalAmount(cb, bill);
            Expression<BigDecimal> settled = cb.sum(
                    receivedAmount(cb, query, bill),
                    support.returnedAmount(cb, query, bill, null));

            predicates.add(PENDING.equals(pendingStatus)
                    ? cb.greaterThan(total, settled)
                    : cb.lessThanOrEqualTo(total, settled));
        }

        return predicates;
    }


    private Expression<BigDecimal> totalAmount(CriteriaBuilder cb, From<?, Billing> bill) {
        return cb.coalesce(support.billAmount(cb, bill), BigDecimal.ZERO);
    }


    // Sum of the payments recorded against the bill, zero when there are none.
    private Expression<BigDecimal> receivedAmount(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            From<?, Billing> bill) {

        Subquery<BigDecimal> received = query.subquery(BigDecimal.class);
        Root<BillingPayment> payment = received.from(BillingPayment.class);

        received.select(cb.coalesce(cb.sum(payment.<BigDecimal>get("receivedAmount")), BigDecimal.ZERO))
                .where(cb.equal(payment.get("billing"), bill));

        return received;
    }


    // null means ALL.
    private String resolvePendingStatus(FinancialReportFilter filter) {

        if (!hasText(filter.getPendingStatus())) {
            return null;
        }

        return switch (filter.getPendingStatus().trim().toUpperCase().replace(' ', '_')) {
            case "ALL" -> null;
            case "PENDING" -> PENDING;
            case "FULLY_PAID", "PAID" -> FULLY_PAID;
            default -> throw new IllegalArgumentException(
                    "Unknown pendingStatus: " + filter.getPendingStatus() + ". Use PENDING, FULLY_PAID or ALL.");
        };
    }


    private BillDueSummaryRowDto toRow(Tuple tuple) {

        BillDueSummaryRowDto row = new BillDueSummaryRowDto();

        row.setPharmacyId(tuple.get("pharmacyId", String.class));
        row.setPharmacyName(tuple.get("pharmacyName", String.class));
        row.setBillingId(tuple.get("billingId", Long.class));
        row.setBillNo(tuple.get("billNo", String.class));
        row.setBilledDate(tuple.get("billedDate", LocalDateTime.class));
        row.setCustomerId(tuple.get("customerId", Long.class));
        row.setPatientName(tuple.get("patientName", String.class));
        row.setPatientMobileNo(tuple.get("patientMobileNo", String.class));
        row.setPatientNumber(tuple.get("patientNumber", String.class));
        row.setIpNumber(tuple.get("ipNumber", String.class));
        row.setDoctorId(tuple.get("doctorId", Long.class));
        row.setReferredBy(tuple.get("referredBy", String.class));

        BigDecimal total = orZero(tuple.get("totalAmount", BigDecimal.class));
        BigDecimal received = orZero(tuple.get("amountReceived", BigDecimal.class));
        BigDecimal returned = orZero(tuple.get("returnedAmount", BigDecimal.class));
        BigDecimal pending = pending(total, received, returned);

        row.setTotalAmount(total);
        row.setAmountReceived(received);
        row.setReturnedAmount(returned);
        row.setPendingAmount(pending);
        row.setStatus(pending.signum() == 0 ? FULLY_PAID : PENDING);

        return row;
    }
}
