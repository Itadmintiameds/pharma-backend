package tiameds.pharmabackend.service.impl.financialReport.doctorWiseSales;

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
import tiameds.pharmabackend.dto.financialReport.doctorWiseSales.DoctorWiseSalesKpiDto;
import tiameds.pharmabackend.dto.financialReport.doctorWiseSales.DoctorWiseSalesRowDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingDetails;
import tiameds.pharmabackend.entity.billing.DoctorDetails;
import tiameds.pharmabackend.entity.billing.SalesReturn;
import tiameds.pharmabackend.entity.billing.SalesReturnDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.SalesReturnStatus;
import tiameds.pharmabackend.service.financialReport.doctorWiseSales.DoctorWiseSalesService;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport;
import tiameds.pharmabackend.service.impl.financialReport.common.FinancialReportSupport.DateRange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Doctor-wise / referred-by sales. Sales (by bill date) and returns (by return
 * date, like the bill summary) are each grouped by pharmacy + doctor + product
 * in the database, then merged here so a product returned in the period but
 * sold earlier still gets a row. Only bills with a referring doctor count.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorWiseSalesServiceImpl implements DoctorWiseSalesService {

    // Pharmacies, then doctors, then products, alphabetically.
    private static final Comparator<DoctorWiseSalesRowDto> ROW_ORDER = Comparator
            .comparing(DoctorWiseSalesRowDto::getPharmacyName, byName())
            .thenComparing(DoctorWiseSalesRowDto::getPharmacyId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(DoctorWiseSalesRowDto::getReferredBy, byName())
            .thenComparing(DoctorWiseSalesRowDto::getDoctorId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(DoctorWiseSalesRowDto::getProductName, byName())
            .thenComparing(DoctorWiseSalesRowDto::getProductId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final FinancialReportSupport support;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public FinancialReportResponse<DoctorWiseSalesKpiDto, DoctorWiseSalesRowDto> getDoctorWiseSales(
            FinancialReportFilter filter,
            UserDetails user) {

        List<String> pharmacyIds = support.resolvePharmacyIds(filter, user);

        DateRange range = support.resolveDateRange(filter);
        List<CustomerType> patientTypes = support.resolvePatientTypes(filter);
        int page = support.resolvePage(filter);
        int size = support.resolveSize(filter);

        Map<RowKey, DoctorWiseSalesRowDto> rows = new HashMap<>();

        // Sale and return values include round-off, like the amounts actually
        // collected and refunded. Round-off is stored on the bill / return, so
        // each row takes its share by amount.
        for (Tuple sale : findSales(pharmacyIds, range, patientTypes, filter)) {
            DoctorWiseSalesRowDto row = rowFor(rows, sale);
            row.setSaleQuantity(row.getSaleQuantity() + sale.get("quantity", Long.class));
            row.setSaleValue(row.getSaleValue()
                    .add(sale.get("amount", BigDecimal.class))
                    .add(sale.get("roundOff", BigDecimal.class)));
        }

        List<Tuple> returns = findReturns(pharmacyIds, range, patientTypes, filter);
        Map<Long, BigDecimal> returnLineTotals = returnLineTotals(returns);

        for (Tuple saleReturn : returns) {
            DoctorWiseSalesRowDto row = rowFor(rows, saleReturn);
            BigDecimal amount = saleReturn.get("amount", BigDecimal.class);

            row.setReturnQuantity(row.getReturnQuantity() + saleReturn.get("quantity", Long.class));
            row.setReturnValue(row.getReturnValue()
                    .add(amount)
                    .add(returnRoundOffShare(saleReturn, amount, returnLineTotals)));
        }

        List<DoctorWiseSalesRowDto> sorted = new ArrayList<>(rows.values());
        sorted.forEach(row -> {
            row.setSaleValue(row.getSaleValue().setScale(2, RoundingMode.HALF_UP));
            row.setReturnValue(row.getReturnValue().setScale(2, RoundingMode.HALF_UP));
            row.setNetSaleValue(row.getSaleValue().subtract(row.getReturnValue()));
        });
        sorted.sort(ROW_ORDER);
        support.checkPrintLimit(filter, sorted.size());

        int from = (int) Math.min((long) page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());

        ReportPageDto<DoctorWiseSalesRowDto> billDetails = new ReportPageDto<>(
                sorted.subList(from, to),
                page,
                size,
                sorted.size(),
                (sorted.size() + size - 1) / size);

        return new FinancialReportResponse<>(
                FinancialReportSupport.generatedBy(user),
                LocalDateTime.now(),
                buildKpi(sorted),
                billDetails);
    }


    // Bill lines in the period, summed per pharmacy + doctor + product.
    private List<Tuple> findSales(
            List<String> pharmacyIds,
            DateRange range,
            List<CustomerType> patientTypes,
            FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<BillingDetails> line = query.from(BillingDetails.class);
        Join<BillingDetails, Billing> bill = line.join("billing");
        Join<Billing, PharmacyDetails> pharmacy = bill.join("pharmacy");
        Join<Billing, DoctorDetails> doctor = bill.join("doctor");
        Join<BillingDetails, ProductDetails> product = line.join("product");

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(pharmacy.get("pharmacyId").in(pharmacyIds));
        predicates.addAll(support.within(cb, bill.get("createdAt"), range));
        predicates.addAll(sharedPredicates(cb, bill, doctor, product, patientTypes, filter));

        query.multiselect(
                        pharmacy.get("pharmacyId").alias("pharmacyId"),
                        pharmacy.get("pharmacyName").alias("pharmacyName"),
                        doctor.get("doctorId").alias("doctorId"),
                        doctor.get("doctorName").alias("referredBy"),
                        product.get("productId").alias("productId"),
                        product.get("productName").alias("productName"),
                        cb.coalesce(cb.sum(line.<Long>get("billQuantity")), 0L).alias("quantity"),
                        cb.coalesce(cb.sum(line.<BigDecimal>get("netAmount")), BigDecimal.ZERO).alias("amount"),
                        cb.coalesce(cb.sum(roundOffShare(cb, bill, line)), BigDecimal.ZERO).alias("roundOff"))
                .where(predicates.toArray(Predicate[]::new))
                .groupBy(
                        pharmacy.get("pharmacyId"),
                        pharmacy.get("pharmacyName"),
                        doctor.get("doctorId"),
                        doctor.get("doctorName"),
                        product.get("productId"),
                        product.get("productName"));

        return entityManager.createQuery(query).getResultList();
    }


    // Round-off is stored once per bill. Each line takes its share by amount:
    // bill round-off * line net / bill net (the bill net is the sum of its
    // lines), so the lines of a bill add up to the bill's round-off.
    private static Expression<BigDecimal> roundOffShare(
            CriteriaBuilder cb,
            Path<Billing> bill,
            Path<BillingDetails> line) {

        Expression<BigDecimal> billNet = cb.nullif(bill.<BigDecimal>get("totalNetAmount"), BigDecimal.ZERO);

        return cb.quot(
                        cb.prod(cb.coalesce(bill.<BigDecimal>get("roundOffAmount"), BigDecimal.ZERO),
                                line.<BigDecimal>get("netAmount")),
                        billNet)
                .as(BigDecimal.class);
    }


    // Completed return lines dated in the period, summed per pharmacy + the
    // original bill's doctor + product.
    private List<Tuple> findReturns(
            List<String> pharmacyIds,
            DateRange range,
            List<CustomerType> patientTypes,
            FinancialReportFilter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<SalesReturnDetails> returnLine = query.from(SalesReturnDetails.class);
        Join<SalesReturnDetails, SalesReturn> salesReturn = returnLine.join("salesReturn");
        Join<SalesReturn, Billing> bill = salesReturn.join("billing");
        Join<Billing, PharmacyDetails> pharmacy = bill.join("pharmacy");
        Join<Billing, DoctorDetails> doctor = bill.join("doctor");
        Join<SalesReturnDetails, ProductDetails> product = returnLine.join("product");

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(salesReturn.get("pharmacyId").in(pharmacyIds));
        predicates.add(cb.equal(salesReturn.get("salesReturnStatus"), SalesReturnStatus.COMPLETED));
        predicates.addAll(support.within(cb, salesReturn.get("salesReturnDate"), range));
        predicates.addAll(sharedPredicates(cb, bill, doctor, product, patientTypes, filter));

        query.multiselect(
                        pharmacy.get("pharmacyId").alias("pharmacyId"),
                        pharmacy.get("pharmacyName").alias("pharmacyName"),
                        doctor.get("doctorId").alias("doctorId"),
                        doctor.get("doctorName").alias("referredBy"),
                        product.get("productId").alias("productId"),
                        product.get("productName").alias("productName"),
                        cb.coalesce(cb.sum(returnLine.<Long>get("salesReturnQuantity")), 0L).alias("quantity"),
                        cb.coalesce(cb.sum(returnLine.<BigDecimal>get("netAmount")), BigDecimal.ZERO).alias("amount"),
                        salesReturn.get("salesReturnId").alias("salesReturnId"),
                        salesReturn.get("totalNetAmount").alias("returnTotal"))
                .where(predicates.toArray(Predicate[]::new))
                .groupBy(
                        pharmacy.get("pharmacyId"),
                        pharmacy.get("pharmacyName"),
                        doctor.get("doctorId"),
                        doctor.get("doctorName"),
                        product.get("productId"),
                        product.get("productName"),
                        salesReturn.get("salesReturnId"),
                        salesReturn.get("totalNetAmount"));

        return entityManager.createQuery(query).getResultList();
    }


    // Sum of all line amounts of each return, whatever the product filters,
    // to work out how much of the return's total is round-off.
    private Map<Long, BigDecimal> returnLineTotals(List<Tuple> returns) {

        Set<Long> returnIds = new HashSet<>();
        returns.forEach(r -> returnIds.add(r.get("salesReturnId", Long.class)));

        Map<Long, BigDecimal> totals = new HashMap<>();

        if (returnIds.isEmpty()) {
            return totals;
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();

        Root<SalesReturnDetails> returnLine = query.from(SalesReturnDetails.class);
        Join<SalesReturnDetails, SalesReturn> salesReturn = returnLine.join("salesReturn");

        query.multiselect(
                        salesReturn.get("salesReturnId").alias("salesReturnId"),
                        cb.coalesce(cb.sum(returnLine.<BigDecimal>get("netAmount")), BigDecimal.ZERO).alias("lines"))
                .where(salesReturn.get("salesReturnId").in(returnIds))
                .groupBy(salesReturn.get("salesReturnId"));

        for (Tuple tuple : entityManager.createQuery(query).getResultList()) {
            totals.put(tuple.get("salesReturnId", Long.class), tuple.get("lines", BigDecimal.class));
        }

        return totals;
    }


    // A return's total can include round-off that its lines do not (the
    // refund is rounded like the bill was): round-off = return total - sum of
    // its lines, split across the return's rows by amount. Zero for returns
    // whose total equals their lines.
    private static BigDecimal returnRoundOffShare(
            Tuple saleReturn,
            BigDecimal rowAmount,
            Map<Long, BigDecimal> returnLineTotals) {

        BigDecimal returnTotal = saleReturn.get("returnTotal", BigDecimal.class);
        BigDecimal lines = returnLineTotals.get(saleReturn.get("salesReturnId", Long.class));

        if (returnTotal == null || lines == null || lines.signum() == 0) {
            return BigDecimal.ZERO;
        }

        return returnTotal.subtract(lines)
                .multiply(rowAmount)
                .divide(lines, 10, RoundingMode.HALF_UP);
    }


    // Referred-by, patient type and product / HSN / GST filters, applied the
    // same way to sales and returns.
    private List<Predicate> sharedPredicates(
            CriteriaBuilder cb,
            From<?, Billing> bill,
            From<?, DoctorDetails> doctor,
            From<?, ProductDetails> product,
            List<CustomerType> patientTypes,
            FinancialReportFilter filter) {

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getDoctorIds() != null && !filter.getDoctorIds().isEmpty()) {
            predicates.add(doctor.get("doctorId").in(filter.getDoctorIds()));
        }

        if (!patientTypes.isEmpty()) {
            predicates.add(bill.get("customerType").in(patientTypes));
        }

        predicates.addAll(support.productPredicates(cb, product, filter));

        return predicates;
    }


    private static RowKey keyOf(Tuple tuple) {
        return new RowKey(
                tuple.get("pharmacyId", String.class),
                tuple.get("doctorId", Long.class),
                tuple.get("productId", String.class));
    }


    private DoctorWiseSalesRowDto rowFor(Map<RowKey, DoctorWiseSalesRowDto> rows, Tuple tuple) {

        return rows.computeIfAbsent(keyOf(tuple), k -> {
            DoctorWiseSalesRowDto row = new DoctorWiseSalesRowDto();
            row.setPharmacyId(k.pharmacyId());
            row.setPharmacyName(tuple.get("pharmacyName", String.class));
            row.setDoctorId(k.doctorId());
            row.setReferredBy(tuple.get("referredBy", String.class));
            row.setProductId(k.productId());
            row.setProductName(tuple.get("productName", String.class));
            row.setSaleQuantity(0L);
            row.setReturnQuantity(0L);
            row.setSaleValue(BigDecimal.ZERO);
            row.setReturnValue(BigDecimal.ZERO);
            return row;
        });
    }


    private DoctorWiseSalesKpiDto buildKpi(List<DoctorWiseSalesRowDto> rows) {

        DoctorWiseSalesKpiDto kpi = new DoctorWiseSalesKpiDto();

        kpi.setTotalSaleValue(rows.stream()
                .map(DoctorWiseSalesRowDto::getSaleValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        kpi.setTotalReturnValue(rows.stream()
                .map(DoctorWiseSalesRowDto::getReturnValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        kpi.setNetSaleValue(kpi.getTotalSaleValue().subtract(kpi.getTotalReturnValue()));

        return kpi;
    }


    private static Comparator<String> byName() {
        return Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    }


    private record RowKey(String pharmacyId, Long doctorId, String productId) {
    }
}
