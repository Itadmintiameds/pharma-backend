package tiameds.pharmabackend.service.impl.financialReport.billSummary;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillDetailedSalesKpiDto;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillDetailedSalesRowDto;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillSummaryKpiDto;
import tiameds.pharmabackend.dto.financialReport.billSummary.BillSummaryRowDto;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportResponse;
import tiameds.pharmabackend.dto.financialReport.common.ReportPageDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingDetails;
import tiameds.pharmabackend.entity.billing.CustomerManagement;
import tiameds.pharmabackend.entity.billing.DoctorDetails;
import tiameds.pharmabackend.entity.billing.SalesReturn;
import tiameds.pharmabackend.entity.master.PurchaseSmallestUnit;
import tiameds.pharmabackend.entity.product.BatchDetails;
import tiameds.pharmabackend.entity.product.PackagingDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.service.financialReport.billSummary.BillSummaryService;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.DateRange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillSummaryServiceImpl implements BillSummaryService {

    private final FinancialReportSupport support;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public FinancialReportResponse<BillSummaryKpiDto, BillSummaryRowDto> getBillSummary(
            FinancialReportFilter filter,
            UserDetails user) {

        List<String> pharmacyIds = support.resolvePharmacyIds(filter, user);

        DateRange range = support.resolveDateRange(filter);
        int page = support.resolvePage(filter);
        int size = support.resolveSize(filter);

        long totalElements = countBills(pharmacyIds, range, filter);

        List<BillSummaryRowDto> rows = totalElements == 0
                ? List.of()
                : findBills(pharmacyIds, range, filter, page, size);

        return new FinancialReportResponse<>(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                buildKpi(pharmacyIds, range, filter),
                toPage(rows, page, size, totalElements));
    }


    @Override
    public FinancialReportResponse<BillDetailedSalesKpiDto, BillDetailedSalesRowDto> getBillDetailedSales(
            FinancialReportFilter filter,
            UserDetails user) {

        List<String> pharmacyIds = support.resolvePharmacyIds(filter, user);

        DateRange range = support.resolveDateRange(filter);
        int page = support.resolvePage(filter);
        int size = support.resolveSize(filter);

        long totalElements = countBillLines(pharmacyIds, range, filter);

        List<BillDetailedSalesRowDto> rows = totalElements == 0
                ? List.of()
                : findBillLines(pharmacyIds, range, filter, page, size);

        return new FinancialReportResponse<>(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                buildLineKpi(pharmacyIds, range, filter),
                toPage(rows, page, size, totalElements));
    }


    private List<BillSummaryRowDto> findBills(
            List<String> pharmacyIds,
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
                support.billAmount(cb, bill).alias("totalAmount"));

        query.where(billPredicates(cb, bill, customer, doctor, pharmacyIds, range, filter)
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


    private long countBills(List<String> pharmacyIds, DateRange range, FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<Billing> bill = query.from(Billing.class);
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        query.select(cb.count(bill))
                .where(billPredicates(cb, bill, customer, doctor, pharmacyIds, range, filter)
                        .toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    private List<BillDetailedSalesRowDto> findBillLines(
            List<String> pharmacyIds,
            DateRange range,
            FinancialReportFilter filter,
            int page,
            int size) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<BillingDetails> line = query.from(BillingDetails.class);
        Join<BillingDetails, Billing> bill = line.join("billing");
        Join<Billing, PharmacyDetails> pharmacy = bill.join("pharmacy");
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);
        Join<BillingDetails, ProductDetails> product = line.join("product", JoinType.LEFT);
        Join<BillingDetails, BatchDetails> batch = line.join("batch", JoinType.LEFT);
        Join<BatchDetails, PackagingDetails> packaging = batch.join("packagingDetails", JoinType.LEFT);
        Join<PackagingDetails, PurchaseSmallestUnit> smallestUnit =
                packaging.join("purchaseSmallestUnit", JoinType.LEFT);

        query.multiselect(
                bill.get("billingId").alias("billingId"),
                line.get("billingDetailsId").alias("billingDetailsId"),
                bill.get("billNo").alias("billNo"),
                bill.get("createdAt").alias("billedDate"),
                pharmacy.get("pharmacyId").alias("pharmacyId"),
                pharmacy.get("pharmacyName").alias("pharmacyName"),
                customer.get("customerId").alias("customerId"),
                customer.get("customerName").alias("patientName"),
                customer.get("customerPhoneNo").alias("patientMobileNo"),
                doctor.get("doctorId").alias("doctorId"),
                doctor.get("doctorName").alias("referredBy"),
                product.get("productId").alias("productId"),
                product.get("productName").alias("productName"),
                batch.get("batchId").alias("batchId"),
                batch.get("batchNumber").alias("batchNumber"),
                line.get("billQuantity").alias("quantity"),
                line.get("unit").alias("unit"),
                packaging.get("purchaseUnit").alias("purchaseUnit"),
                smallestUnit.get("purchaseSmallestUnitName").alias("smallestSaleUnit"),
                product.get("hsnNo").alias("hsnNo"),
                product.get("gstPercentage").alias("gstPercentage"),
                line.get("grossAmount").alias("grossAmount"),
                line.get("discountAmount").alias("discountAmount"),
                line.get("gstAmount").alias("gstAmount"),
                line.get("netAmount").alias("netAmount"));

        List<Predicate> predicates = billPredicates(cb, bill, customer, doctor, pharmacyIds, range, filter);
        predicates.addAll(support.productPredicates(cb, product, filter));

        query.where(predicates.toArray(Predicate[]::new));

        // One pharmacy's lines after another, newest bill first within each.
        query.orderBy(
                cb.asc(pharmacy.get("pharmacyName")),
                cb.asc(pharmacy.get("pharmacyId")),
                cb.desc(bill.get("createdAt")),
                cb.desc(bill.get("billingId")),
                cb.asc(line.get("billingDetailsId")));

        return entityManager.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(this::toLineRow)
                .toList();
    }


    private long countBillLines(List<String> pharmacyIds, DateRange range, FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        Root<BillingDetails> line = query.from(BillingDetails.class);
        Join<BillingDetails, Billing> bill = line.join("billing");
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);
        Join<BillingDetails, ProductDetails> product = line.join("product", JoinType.LEFT);

        List<Predicate> predicates = billPredicates(cb, bill, customer, doctor, pharmacyIds, range, filter);
        predicates.addAll(support.productPredicates(cb, product, filter));

        query.select(cb.count(line)).where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    // KPIs for the line-level report: totals of the matching lines, so a
    // product / HSN / GST filter narrows the figures too.
    private BillDetailedSalesKpiDto buildLineKpi(
            List<String> pharmacyIds,
            DateRange range,
            FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<BillingDetails> line = query.from(BillingDetails.class);
        Join<BillingDetails, Billing> bill = line.join("billing");
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);
        Join<BillingDetails, ProductDetails> product = line.join("product", JoinType.LEFT);

        List<Predicate> predicates = billPredicates(cb, bill, customer, doctor, pharmacyIds, range, filter);
        predicates.addAll(support.productPredicates(cb, product, filter));

        query.multiselect(
                        sumOrZero(cb, line.get("grossAmount")).alias("grossAmount"),
                        sumOrZero(cb, line.get("discountAmount")).alias("discountAmount"),
                        sumOrZero(cb, line.get("gstAmount")).alias("gstAmount"),
                        sumOrZero(cb, line.get("netAmount")).alias("netAmount"))
                .where(predicates.toArray(Predicate[]::new));

        Tuple totals = entityManager.createQuery(query).getSingleResult();

        BillDetailedSalesKpiDto kpi = new BillDetailedSalesKpiDto();
        kpi.setTotalGrossAmount(totals.get("grossAmount", BigDecimal.class));
        kpi.setTotalDiscountAmount(totals.get("discountAmount", BigDecimal.class));
        kpi.setTotalGstAmount(totals.get("gstAmount", BigDecimal.class));
        kpi.setTotalNetAmount(totals.get("netAmount", BigDecimal.class));

        return kpi;
    }


    private Expression<BigDecimal> sumOrZero(CriteriaBuilder cb, Path<BigDecimal> column) {
        return cb.coalesce(cb.sum(column), BigDecimal.ZERO);
    }


    private <T> ReportPageDto<T> toPage(List<T> rows, int page, int size, long totalElements) {

        return new ReportPageDto<>(
                rows,
                page,
                size,
                totalElements,
                (int) ((totalElements + size - 1) / size));
    }


    private BillSummaryKpiDto buildKpi(List<String> pharmacyIds, DateRange range, FinancialReportFilter filter) {

        BigDecimal totalSales = sumSales(pharmacyIds, range, filter);
        BigDecimal totalReturns = sumReturns(pharmacyIds, range, filter);

        BillSummaryKpiDto kpi = new BillSummaryKpiDto();
        kpi.setTotalSalesAmount(totalSales);
        kpi.setTotalSalesReturnAmount(totalReturns);
        kpi.setNetSalesAmount(totalSales.subtract(totalReturns));

        return kpi;
    }


    private BigDecimal sumSales(List<String> pharmacyIds, DateRange range, FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);

        Root<Billing> bill = query.from(Billing.class);
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        query.select(cb.coalesce(cb.sum(support.billAmount(cb, bill)), BigDecimal.ZERO))
                .where(billPredicates(cb, bill, customer, doctor, pharmacyIds, range, filter)
                        .toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    // Returns are counted by the date they were made, so a return this month
    // against last month's bill reduces this month's net sales. The search and
    // referred-by filters still apply through the bill the return belongs to.
    private BigDecimal sumReturns(List<String> pharmacyIds, DateRange range, FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);

        Root<SalesReturn> salesReturn = query.from(SalesReturn.class);
        Join<SalesReturn, Billing> bill = salesReturn.join("billing");
        Join<Billing, CustomerManagement> customer = bill.join("customer", JoinType.LEFT);
        Join<Billing, DoctorDetails> doctor = bill.join("doctor", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(salesReturn.get("pharmacyId").in(pharmacyIds));
        predicates.add(cb.equal(salesReturn.get("salesReturnStatus"), SalesReturnStatus.COMPLETED));
        predicates.addAll(support.within(cb, salesReturn.get("salesReturnDate"), range));
        predicates.addAll(support.billSearchPredicates(cb, bill, customer, doctor, filter));

        query.select(cb.coalesce(cb.sum(salesReturn.<BigDecimal>get("totalNetAmount")), BigDecimal.ZERO))
                .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(query).getSingleResult();
    }


    private List<Predicate> billPredicates(
            CriteriaBuilder cb,
            From<?, Billing> bill,
            Join<Billing, CustomerManagement> customer,
            Join<Billing, DoctorDetails> doctor,
            List<String> pharmacyIds,
            DateRange range,
            FinancialReportFilter filter) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(bill.get("pharmacy").get("pharmacyId").in(pharmacyIds));
        predicates.addAll(support.within(cb, bill.<LocalDateTime>get("createdAt"), range));
        predicates.addAll(support.billSearchPredicates(cb, bill, customer, doctor, filter));

        return predicates;
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


    private BillDetailedSalesRowDto toLineRow(Tuple tuple) {

        BillDetailedSalesRowDto row = new BillDetailedSalesRowDto();

        row.setBillingId(tuple.get("billingId", Long.class));
        row.setBillingDetailsId(tuple.get("billingDetailsId", Long.class));
        row.setBillNo(tuple.get("billNo", String.class));
        row.setBilledDate(tuple.get("billedDate", LocalDateTime.class));
        row.setPharmacyId(tuple.get("pharmacyId", String.class));
        row.setPharmacyName(tuple.get("pharmacyName", String.class));
        row.setCustomerId(tuple.get("customerId", Long.class));
        row.setPatientName(tuple.get("patientName", String.class));
        row.setPatientMobileNo(tuple.get("patientMobileNo", String.class));
        row.setDoctorId(tuple.get("doctorId", Long.class));
        row.setReferredBy(tuple.get("referredBy", String.class));
        row.setProductId(tuple.get("productId", String.class));
        row.setProductName(tuple.get("productName", String.class));
        row.setBatchId(tuple.get("batchId", String.class));
        row.setBatchNumber(tuple.get("batchNumber", String.class));
        row.setQuantity(tuple.get("quantity", Long.class));
        row.setUnit(tuple.get("unit", String.class));
        row.setPurchaseUnit(tuple.get("purchaseUnit", String.class));
        row.setSmallestSaleUnit(tuple.get("smallestSaleUnit", String.class));
        row.setHsnNo(tuple.get("hsnNo", String.class));
        row.setGstPercentage(tuple.get("gstPercentage", String.class));
        row.setGrossAmount(tuple.get("grossAmount", BigDecimal.class));
        row.setDiscountAmount(tuple.get("discountAmount", BigDecimal.class));
        row.setNetAmount(tuple.get("netAmount", BigDecimal.class));

        // CGST takes the half rounded to paise and SGST the remainder, so the
        // two always add back up to the line's GST amount.
        BigDecimal gstAmount = tuple.get("gstAmount", BigDecimal.class);

        if (gstAmount != null) {
            BigDecimal cgst = gstAmount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            row.setCgstAmount(cgst);
            row.setSgstAmount(gstAmount.subtract(cgst));
        }

        return row;
    }
}
