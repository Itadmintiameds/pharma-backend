package tiameds.pharmabackend.service.impl.financialReport.salesSummary;

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
import tiameds.pharmabackend.dto.financialReport.salesSummary.SalesSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.salesSummary.SalesSummaryRowDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingPayment;
import tiameds.pharmabackend.entity.billing.CustomerManagement;
import tiameds.pharmabackend.entity.billing.DoctorDetails;
import tiameds.pharmabackend.entity.billing.SalesReturn;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.PaymentMode;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.service.financialReport.salesSummary.SalesSummaryService;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.DateRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sales summary: five cumulative figures across the selected pharmacies and
 * period. Bills count by bill date and returns by return date, like the bill
 * summary. IP pending is what is still owed on the period's IP bills, counting
 * only payments received up to the end of the period.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesSummaryServiceImpl implements SalesSummaryService {

    private final FinancialReportSupport support;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public FinancialReportResponse<SalesSummaryKpiDto, SalesSummaryRowDto> getSalesSummary(
            FinancialReportFilter filter,
            UserDetails user) {

        Scope scope = new Scope(
                support.resolvePharmacyIds(filter, user),
                support.resolveDateRange(filter),
                support.resolvePatientTypes(filter),
                support.resolvePaymentModes(filter),
                filter);

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


    // Bill total and discount over the period's eligible bills.
    private Tuple sumSales(Scope scope) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<Billing> bill = query.from(Billing.class);

        List<Predicate> predicates = eligibleBill(cb, query, bill, scope);
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

        List<Predicate> predicates = eligibleBill(cb, query, bill, scope);
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

        List<Predicate> billed = eligibleBill(cb, billedQuery, bill, scope);
        billed.add(cb.equal(bill.get("customerType"), CustomerType.IP_PATIENT));
        billed.addAll(support.within(cb, bill.get("createdAt"), scope.range()));

        billedQuery.select(cb.coalesce(cb.sum(support.billAmount(cb, bill)), BigDecimal.ZERO))
                .where(billed.toArray(Predicate[]::new));

        CriteriaQuery<BigDecimal> receivedQuery = cb.createQuery(BigDecimal.class);
        Root<BillingPayment> payment = receivedQuery.from(BillingPayment.class);
        Join<BillingPayment, Billing> paidBill = payment.join("billing");

        List<Predicate> received = eligibleBill(cb, receivedQuery, paidBill, scope);
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


    // Every filter except the date, which each figure applies to its own column.
    private List<Predicate> eligibleBill(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            From<?, Billing> bill,
            Scope scope) {

        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(bill.get("pharmacy").get("pharmacyId").in(scope.pharmacyIds()));
        predicates.addAll(support.billSearchPredicates(cb, bill, customer, doctor, scope.filter()));

        if (!scope.patientTypes().isEmpty()) {
            predicates.add(bill.get("customerType").in(scope.patientTypes()));
        }

        if (!scope.paymentModes().isEmpty()) {
            predicates.add(support.paidWithAnyOf(cb, query, bill, scope.paymentModes()));
        }

        return predicates;
    }


    private record Scope(
            List<String> pharmacyIds,
            DateRange range,
            List<CustomerType> patientTypes,
            List<PaymentMode> paymentModes,
            FinancialReportFilter filter) {
    }
}
