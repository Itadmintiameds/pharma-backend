package tiameds.pharmabackend.service.impl.financialReport.pharmacyReceipts;

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
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.PaymentModeSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.PaymentModeSummaryRowDto;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.ReceiptDetailsKpiDto;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.ReceiptDetailsRowDto;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.SalesSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.pharmacyReceipts.SalesSummaryRowDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingPayment;
import tiameds.pharmabackend.entity.billing.CustomerManagement;
import tiameds.pharmabackend.entity.billing.DoctorDetails;
import tiameds.pharmabackend.entity.billing.SalesReturn;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.PaymentMode;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.service.financialReport.pharmacyReceipts.PharmacyReceiptsService;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.DateRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Pharmacy receipts reports.
 * <p>
 * Sales summary: five cumulative figures. Bills count by bill date and
 * returns by return date, like the bill summary. IP pending is what is still
 * owed on the period's IP bills, counting only payments received up to the
 * end of the period.
 * <p>
 * Payment mode summary: money received in the period (by payment date),
 * split by payment mode.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PharmacyReceiptsServiceImpl implements PharmacyReceiptsService {

    // Row order and labels of the payment mode summary. Credit is left out:
    // a credit entry is recorded with nothing received.
    private static final Map<PaymentMode, String> PAYMENT_MODE_LABELS = new EnumMap<>(Map.of(
            PaymentMode.CARD, "Card",
            PaymentMode.CASH, "Cash",
            PaymentMode.UPI, "UPI"));

    private static final List<PaymentMode> PAYMENT_MODE_ORDER = List.of(
            PaymentMode.CARD,
            PaymentMode.CASH,
            PaymentMode.UPI);

    private final FinancialReportSupport support;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public FinancialReportResponse<SalesSummaryKpiDto, SalesSummaryRowDto> getSalesSummary(
            FinancialReportFilter filter,
            UserDetails user) {

        Scope scope = resolveScope(filter, user);

        Tuple sales = sumSales(scope);
        BigDecimal totalSales = sales.get("sales", BigDecimal.class);
        BigDecimal totalDiscount = sales.get("discount", BigDecimal.class);
        BigDecimal totalReturns = sumReturns(scope);
        BigDecimal netSales = totalSales.subtract(totalReturns);
        BigDecimal ipPending = ipPendingAmount(scope);

        SalesSummaryKpiDto kpi = new SalesSummaryKpiDto();
        kpi.setTotalSales(totalSales);
        kpi.setTotalSalesReturn(totalReturns);
        kpi.setNetSales(netSales);
        kpi.setIpPendingAmount(ipPending);

        List<SalesSummaryRowDto> rows = List.of(
                new SalesSummaryRowDto("TOTAL_SALES", "Total Sales", "Pharmacy Bill",
                        "Sum of eligible finalized bill Total Amount", totalSales),
                new SalesSummaryRowDto("TOTAL_DISCOUNT", "Total Discount", "Pharmacy Bill",
                        "Sum of discounts applied to eligible bills", totalDiscount),
                new SalesSummaryRowDto("TOTAL_SALES_RETURN", "Total Sales Return", "Sales Return",
                        "Sum of eligible finalized Sales Return amounts", totalReturns),
                new SalesSummaryRowDto("NET_SALES", "Net Sales", "Calculated",
                        "Total Sales - Total Sales Return", netSales),
                new SalesSummaryRowDto("IP_PENDING_AMOUNT", "IP Pending Amount", "IP Bill / Payment Records",
                        "Outstanding amount against eligible IP bills, as of reporting end date", ipPending));

        return new FinancialReportResponse<>(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                kpi,
                new ReportPageDto<>(rows, 0, rows.size(), rows.size(), 1));
    }


    @Override
    public FinancialReportResponse<PaymentModeSummaryKpiDto, PaymentModeSummaryRowDto> getPaymentModeSummary(
            FinancialReportFilter filter,
            UserDetails user) {

        Scope scope = resolveScope(filter, user);

        Map<PaymentMode, BigDecimal> received = sumReceivedByMode(scope);

        // Every mode when none is selected, otherwise just the selected ones.
        List<PaymentMode> shown = PAYMENT_MODE_ORDER.stream()
                .filter(mode -> scope.paymentModes().isEmpty() || scope.paymentModes().contains(mode))
                .toList();

        List<PaymentModeSummaryRowDto> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (PaymentMode mode : shown) {
            BigDecimal amount = received.getOrDefault(mode, BigDecimal.ZERO);
            rows.add(new PaymentModeSummaryRowDto(mode.name(), PAYMENT_MODE_LABELS.get(mode), amount));
            total = total.add(amount);
        }

        rows.add(new PaymentModeSummaryRowDto("TOTAL", "Total", total));

        PaymentModeSummaryKpiDto kpi = new PaymentModeSummaryKpiDto();
        kpi.setTotalReceived(total);
        kpi.setCard(received.getOrDefault(PaymentMode.CARD, BigDecimal.ZERO));
        kpi.setCash(received.getOrDefault(PaymentMode.CASH, BigDecimal.ZERO));
        kpi.setUpi(received.getOrDefault(PaymentMode.UPI, BigDecimal.ZERO));

        return new FinancialReportResponse<>(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                kpi,
                new ReportPageDto<>(rows, 0, rows.size(), rows.size(), 1));
    }


    @Override
    public FinancialReportResponse<ReceiptDetailsKpiDto, ReceiptDetailsRowDto> getReceiptDetails(
            FinancialReportFilter filter,
            UserDetails user) {

        Scope scope = resolveScope(filter, user, support.resolveBoundedDateRange(filter));

        Tuple receipts = sumReceiptsByBillAge(scope);
        BigDecimal current = receipts.get("current", BigDecimal.class);
        BigDecimal previous = receipts.get("previous", BigDecimal.class);
        BigDecimal total = current.add(previous);

        ReceiptDetailsKpiDto kpi = new ReceiptDetailsKpiDto();
        kpi.setTotalReceipts(total);
        kpi.setCurrentBillReceipts(current);
        kpi.setPreviousBillReceipts(previous);

        List<ReceiptDetailsRowDto> rows = List.of(
                new ReceiptDetailsRowDto("CURRENT_BILL_RECEIPTS", "Current Bill Receipts",
                        "Pharmacy Bill / Payment Records",
                        "Payments received during the selected period against bills generated during the same period",
                        current),
                new ReceiptDetailsRowDto("PREVIOUS_BILL_RECEIPTS", "Previous Bill Receipts",
                        "Payment Records",
                        "Payments received during the selected period against bills generated before the period",
                        previous),
                new ReceiptDetailsRowDto("TOTAL_RECEIPTS", "Total Receipts", "Calculated",
                        "Current Bill Receipts + Previous Bill Receipts", total));

        return new FinancialReportResponse<>(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                kpi,
                new ReportPageDto<>(rows, 0, rows.size(), rows.size(), 1));
    }


    // Bill total and discount over the period's eligible bills.
    private Tuple sumSales(Scope scope) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<Billing> bill = query.from(Billing.class);

        List<Predicate> predicates = eligibleBill(cb, query, bill, scope, true);
        predicates.addAll(support.within(cb, bill.get("createdAt"), scope.range()));

        query.multiselect(
                        cb.coalesce(cb.sum(support.billAmount(cb, bill)), BigDecimal.ZERO).alias("sales"),
                        cb.coalesce(cb.sum(bill.<BigDecimal>get("totalDiscountAmount")), BigDecimal.ZERO)
                                .alias("discount"))
                .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    // Completed returns dated in the period, against eligible bills.
    private BigDecimal sumReturns(Scope scope) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);

        Root<SalesReturn> salesReturn = query.from(SalesReturn.class);
        Join<SalesReturn, Billing> bill = salesReturn.join("billing");

        List<Predicate> predicates = eligibleBill(cb, query, bill, scope, true);
        predicates.add(cb.equal(salesReturn.get("salesReturnStatus"), SalesReturnStatus.COMPLETED));
        predicates.addAll(support.within(cb, salesReturn.get("salesReturnDate"), scope.range()));

        query.select(cb.coalesce(cb.sum(salesReturn.<BigDecimal>get("totalNetAmount")), BigDecimal.ZERO))
                .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    // Billed on the period's eligible IP bills minus what had been received on
    // them by the end of the period. Zero when the patient type filter leaves
    // out IP.
    private BigDecimal ipPendingAmount(Scope scope) {

        if (!scope.patientTypes().isEmpty() && !scope.patientTypes().contains(CustomerType.IP_PATIENT)) {
            return BigDecimal.ZERO;
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<BigDecimal> billedQuery = cb.createQuery(BigDecimal.class);
        Root<Billing> bill = billedQuery.from(Billing.class);

        List<Predicate> billed = eligibleBill(cb, billedQuery, bill, scope, true);
        billed.add(cb.equal(bill.get("customerType"), CustomerType.IP_PATIENT));
        billed.addAll(support.within(cb, bill.get("createdAt"), scope.range()));

        billedQuery.select(cb.coalesce(cb.sum(support.billAmount(cb, bill)), BigDecimal.ZERO))
                .where(billed.toArray(Predicate[]::new));

        CriteriaQuery<BigDecimal> receivedQuery = cb.createQuery(BigDecimal.class);
        Root<BillingPayment> payment = receivedQuery.from(BillingPayment.class);
        Join<BillingPayment, Billing> paidBill = payment.join("billing");

        List<Predicate> received = eligibleBill(cb, receivedQuery, paidBill, scope, true);
        received.add(cb.equal(paidBill.get("customerType"), CustomerType.IP_PATIENT));
        received.addAll(support.within(cb, paidBill.get("createdAt"), scope.range()));

        if (scope.range().endExclusive() != null) {
            received.add(cb.lessThan(payment.get("createdAt"), scope.range().endExclusive()));
        }

        receivedQuery.select(cb.coalesce(cb.sum(payment.<BigDecimal>get("receivedAmount")), BigDecimal.ZERO))
                .where(received.toArray(Predicate[]::new));

        BigDecimal totalBilled = entityManager.createQuery(billedQuery).getSingleResult();
        BigDecimal totalReceived = entityManager.createQuery(receivedQuery).getSingleResult();

        return totalBilled.subtract(totalReceived).max(BigDecimal.ZERO);
    }


    // Payments received in the period on eligible bills, summed per mode. The
    // payment mode filter applies to the payment itself here.
    private Map<PaymentMode, BigDecimal> sumReceivedByMode(Scope scope) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<BillingPayment> payment = query.from(BillingPayment.class);
        Join<BillingPayment, Billing> bill = payment.join("billing");

        List<Predicate> predicates = receivedPayments(cb, query, payment, bill, scope);

        query.multiselect(
                        payment.get("paymentMode").alias("mode"),
                        cb.coalesce(cb.sum(payment.<BigDecimal>get("receivedAmount")), BigDecimal.ZERO)
                                .alias("amount"))
                .where(predicates.toArray(Predicate[]::new))
                .groupBy(payment.get("paymentMode"));

        Map<PaymentMode, BigDecimal> received = new EnumMap<>(PaymentMode.class);

        for (Tuple row : entityManager.createQuery(query).getResultList()) {
            PaymentMode mode = row.get("mode", PaymentMode.class);
            if (mode != null) {
                received.put(mode, row.get("amount", BigDecimal.class));
            }
        }

        return received;
    }


    // Payments received in the period, summed by whether the bill was raised
    // within the period (current) or before it (previous). Bills dated after
    // the period cannot have been paid within it and are left out.
    private Tuple sumReceiptsByBillAge(Scope scope) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<BillingPayment> payment = query.from(BillingPayment.class);
        Join<BillingPayment, Billing> bill = payment.join("billing");

        List<Predicate> predicates = receivedPayments(cb, query, payment, bill, scope);
        predicates.add(cb.lessThan(bill.get("createdAt"), scope.range().endExclusive()));

        Path<BigDecimal> amount = payment.get("receivedAmount");
        Predicate billedInPeriod = cb.greaterThanOrEqualTo(bill.get("createdAt"), scope.range().start());

        query.multiselect(
                        cb.coalesce(cb.sum(cb.<BigDecimal>selectCase()
                                .when(billedInPeriod, amount)
                                .otherwise(BigDecimal.ZERO)), BigDecimal.ZERO).alias("current"),
                        cb.coalesce(cb.sum(cb.<BigDecimal>selectCase()
                                .when(billedInPeriod, BigDecimal.ZERO)
                                .otherwise(amount)), BigDecimal.ZERO).alias("previous"))
                .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    // Real money received in the period on eligible bills: credit entries are
    // excluded, and the payment mode filter applies to the payment itself.
    private List<Predicate> receivedPayments(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            Root<BillingPayment> payment,
            From<?, Billing> bill,
            Scope scope) {

        List<Predicate> predicates = eligibleBill(cb, query, bill, scope, false);
        predicates.addAll(support.within(cb, payment.get("createdAt"), scope.range()));
        predicates.add(cb.notEqual(payment.get("paymentMode"), PaymentMode.CREDIT));

        if (!scope.paymentModes().isEmpty()) {
            predicates.add(payment.get("paymentMode").in(scope.paymentModes()));
        }

        return predicates;
    }


    // Every filter except the date, which each figure applies to its own
    // column. With byPaymentMode, a bill matches the payment mode filter when
    // any of its payments used a selected mode.
    private List<Predicate> eligibleBill(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            From<?, Billing> bill,
            Scope scope,
            boolean byPaymentMode) {

        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(bill.get("pharmacy").get("pharmacyId").in(scope.pharmacyIds()));
        predicates.addAll(support.billSearchPredicates(cb, bill, customer, doctor, scope.filter()));

        if (!scope.patientTypes().isEmpty()) {
            predicates.add(bill.get("customerType").in(scope.patientTypes()));
        }

        if (byPaymentMode && !scope.paymentModes().isEmpty()) {
            predicates.add(support.paidWithAnyOf(cb, query, bill, scope.paymentModes()));
        }

        return predicates;
    }


    private Scope resolveScope(FinancialReportFilter filter, UserDetails user) {
        return resolveScope(filter, user, support.resolveDateRange(filter));
    }


    private Scope resolveScope(FinancialReportFilter filter, UserDetails user, DateRange range) {

        return new Scope(
                support.resolvePharmacyIds(filter, user),
                range,
                support.resolvePatientTypes(filter),
                support.resolvePaymentModes(filter),
                filter);
    }


    private record Scope(
            List<String> pharmacyIds,
            DateRange range,
            List<CustomerType> patientTypes,
            List<PaymentMode> paymentModes,
            FinancialReportFilter filter) {
    }
}
