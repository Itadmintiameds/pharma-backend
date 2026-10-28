package tiameds.pharmabackend.service.impl.financialReport.billSummary;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillSummaryReportResponse;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillSummaryRowDto;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.ReportPageDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingDetails;
import tiameds.pharmabackend.entity.billing.CustomerManagement;
import tiameds.pharmabackend.entity.billing.DoctorDetails;
import tiameds.pharmabackend.entity.billing.SalesReturn;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.service.financialReport.billSummary.BillSummaryService;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.DateRange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.hasText;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillSummaryServiceImpl implements BillSummaryService {

    private final FinancialReportSupport support;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public BillSummaryReportResponse getBillSummary(FinancialReportFilter filter, UserDetails user) {

        String pharmacyId = support.requirePharmacy(user);

        DateRange range = support.resolveDateRange(filter);
        int page = support.resolvePage(filter);
        int size = support.resolveSize(filter);

        long totalElements = countBills(pharmacyId, range, filter);

        List<BillSummaryRowDto> rows = totalElements == 0
                ? List.of()
                : findBills(pharmacyId, range, filter, page, size);

        ReportPageDto<BillSummaryRowDto> billDetails = new ReportPageDto<>(
                rows,
                page,
                size,
                totalElements,
                (int) ((totalElements + size - 1) / size));

        return new BillSummaryReportResponse(buildKpi(pharmacyId, range, filter), billDetails);
    }


    private List<BillSummaryRowDto> findBills(
            String pharmacyId,
            DateRange range,
            FinancialReportFilter filter,
            int page,
            int size) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<Billing> bill = query.from(Billing.class);
        Join<Billing, PharmacyDetails> pharmacy = bill.join("pharmacy");
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        Subquery<Long> itemCount = query.subquery(Long.class);
        Root<BillingDetails> line = itemCount.from(BillingDetails.class);
        itemCount.select(cb.count(line)).where(cb.equal(line.get("billing"), bill));

        query.multiselect(
                bill.get("billingId").alias("billingId"),
                bill.get("billNo").alias("billNo"),
                bill.get("createdAt").alias("billedDate"),
                pharmacy.get("pharmacyId").alias("pharmacyId"),
                pharmacy.get("pharmacyName").alias("pharmacyName"),
                customer.get("customerId").alias("customerId"),
                customer.get("customerName").alias("patientName"),
                customer.get("customerPhoneNo").alias("patientMobileNo"),
                customer.get("patientNumber").alias("patientNumber"),
                bill.get("opIpNumber").alias("opIpNumber"),
                doctor.get("doctorId").alias("doctorId"),
                doctor.get("doctorName").alias("referredBy"),
                itemCount.alias("itemCount"),
                billAmount(cb, bill).alias("totalAmount"));

        query.where(billPredicates(cb, bill, customer, doctor, pharmacyId, range, filter)
                .toArray(Predicate[]::new));

        query.orderBy(cb.desc(bill.get("createdAt")), cb.desc(bill.get("billingId")));

        return entityManager.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(this::toRow)
                .toList();
    }


    private long countBills(String pharmacyId, DateRange range, FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<Billing> bill = query.from(Billing.class);
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        query.select(cb.count(bill))
                .where(billPredicates(cb, bill, customer, doctor, pharmacyId, range, filter)
                        .toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    private BillSummaryKpiDto buildKpi(String pharmacyId, DateRange range, FinancialReportFilter filter) {

        BigDecimal totalSales = sumSales(pharmacyId, range, filter);
        BigDecimal totalReturns = sumReturns(pharmacyId, range, filter);

        BillSummaryKpiDto kpi = new BillSummaryKpiDto();
        kpi.setTotalSalesAmount(totalSales);
        kpi.setTotalSalesReturnAmount(totalReturns);
        kpi.setNetSalesAmount(totalSales.subtract(totalReturns));

        return kpi;
    }


    private BigDecimal sumSales(String pharmacyId, DateRange range, FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);

        Root<Billing> bill = query.from(Billing.class);
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        query.select(cb.coalesce(cb.sum(billAmount(cb, bill)), BigDecimal.ZERO))
                .where(billPredicates(cb, bill, customer, doctor, pharmacyId, range, filter)
                        .toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    // Returns are counted by the date they were made, so a return this month
    // against last month's bill reduces this month's net sales. The search and
    // referred-by filters still apply through the bill the return belongs to.
    private BigDecimal sumReturns(String pharmacyId, DateRange range, FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);

        Root<SalesReturn> salesReturn = query.from(SalesReturn.class);
        Join<SalesReturn, Billing> bill = salesReturn.join("billing");
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(salesReturn.get("pharmacyId"), pharmacyId));
        predicates.add(cb.equal(salesReturn.get("salesReturnStatus"), SalesReturnStatus.COMPLETED));
        predicates.addAll(support.within(cb, salesReturn.get("salesReturnDate"), range));
        predicates.addAll(searchPredicates(cb, bill, customer, doctor, filter));

        query.select(cb.coalesce(cb.sum(salesReturn.<BigDecimal>get("totalNetAmount")), BigDecimal.ZERO))
                .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    private List<Predicate> billPredicates(
            CriteriaBuilder cb,
            From<?, Billing> bill,
            Join<Billing, CustomerManagement> customer,
            Join<Billing, DoctorDetails> doctor,
            String pharmacyId,
            DateRange range,
            FinancialReportFilter filter) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(bill.get("pharmacy").get("pharmacyId"), pharmacyId));
        predicates.addAll(support.within(cb, bill.<LocalDateTime>get("createdAt"), range));
        predicates.addAll(searchPredicates(cb, bill, customer, doctor, filter));

        return predicates;
    }


    // Patient search, number search and referred-by filters, applied to a bill.
    private List<Predicate> searchPredicates(
            CriteriaBuilder cb,
            From<?, Billing> bill,
            Join<Billing, CustomerManagement> customer,
            Join<Billing, DoctorDetails> doctor,
            FinancialReportFilter filter) {

        List<Predicate> predicates = new ArrayList<>();

        if (hasText(filter.getPatientSearch())) {
            predicates.add(cb.or(
                    support.contains(cb, customer.get("customerName"), filter.getPatientSearch()),
                    support.contains(cb, customer.get("customerPhoneNo"), filter.getPatientSearch())));
        }

        if (hasText(filter.getNumberSearch())) {
            predicates.add(cb.or(
                    support.contains(cb, customer.get("patientNumber"), filter.getNumberSearch()),
                    support.contains(cb, bill.get("opIpNumber"), filter.getNumberSearch()),
                    support.contains(cb, bill.get("billNo"), filter.getNumberSearch())));
        }

        if (filter.getDoctorIds() != null && !filter.getDoctorIds().isEmpty()) {
            predicates.add(doctor.get("doctorId").in(filter.getDoctorIds()));
        }

        return predicates;
    }


    // Amount actually collected; older bills have no round-off figure stored.
    private Expression<BigDecimal> billAmount(CriteriaBuilder cb, From<?, Billing> bill) {

        return cb.coalesce(
                bill.<BigDecimal>get("totalNetAmountAfterRoundOff"),
                bill.<BigDecimal>get("totalNetAmount"));
    }


    private BillSummaryRowDto toRow(Tuple tuple) {

        BillSummaryRowDto row = new BillSummaryRowDto();

        row.setBillingId(tuple.get("billingId", Long.class));
        row.setBillNo(tuple.get("billNo", String.class));
        row.setBilledDate(tuple.get("billedDate", LocalDateTime.class));
        row.setPharmacyId(tuple.get("pharmacyId", String.class));
        row.setPharmacyName(tuple.get("pharmacyName", String.class));
        row.setCustomerId(tuple.get("customerId", Long.class));
        row.setPatientName(tuple.get("patientName", String.class));
        row.setPatientMobileNo(tuple.get("patientMobileNo", String.class));
        row.setPatientNumber(tuple.get("patientNumber", String.class));
        row.setOpIpNumber(tuple.get("opIpNumber", String.class));
        row.setDoctorId(tuple.get("doctorId", Long.class));
        row.setReferredBy(tuple.get("referredBy", String.class));
        row.setItemCount(tuple.get("itemCount", Long.class));
        row.setTotalAmount(tuple.get("totalAmount", BigDecimal.class));

        return row;
    }
}
