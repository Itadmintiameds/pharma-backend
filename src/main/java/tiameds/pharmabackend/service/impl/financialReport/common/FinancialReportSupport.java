package tiameds.pharmabackend.service.impl.financialReport.common;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.billing.Billing;
import tiameds.pharmabackend.entity.billing.BillingPayment;
import tiameds.pharmabackend.entity.billing.CustomerManagement;
import tiameds.pharmabackend.entity.billing.DoctorDetails;
import tiameds.pharmabackend.entity.product.ProductDetails;
import tiameds.pharmabackend.enums.CustomerType;
import tiameds.pharmabackend.enums.PaymentMode;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pieces every financial report needs: resolving the selected pharmacy,
 * turning the date filter into a range, paging limits and search matching.
 */
@Component
@RequiredArgsConstructor
public class FinancialReportSupport {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 500;

    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final CurrentPharmacyContext pharmacyContext;


    /**
     * The pharmacies a report covers: the {@code pharmacyIds} multi-select when
     * sent, otherwise the pharmacy in the X-Pharmacy-Id header. Every one must
     * be mapped to the user.
     */
    public List<String> resolvePharmacyIds(FinancialReportFilter filter, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> requested = trimmed(filter.getPharmacyIds());

        if (requested.isEmpty()) {
            requested = List.of(pharmacyContext.getCurrentPharmacy());
        }

        Set<String> allowed = new HashSet<>(
                pharmacyDetailsRepository.findPharmacyIdsByUserId(persistentUser.getUserId()));

        List<String> denied = requested.stream()
                .filter(id -> !allowed.contains(id))
                .toList();

        if (!denied.isEmpty()) {
            throw new AccessDeniedException(
                    "You are not authorized to use these pharmacies: " + String.join(", ", denied));
        }

        return requested;
    }


    /**
     * A single day when {@code date} is sent, otherwise {@code fromDate} to
     * {@code toDate} inclusive. Either end may be null (open-ended).
     */
    public DateRange resolveDateRange(FinancialReportFilter filter) {

        if (filter.getDate() != null) {
            return DateRange.of(filter.getDate(), filter.getDate());
        }

        if (filter.getFromDate() != null
                && filter.getToDate() != null
                && filter.getFromDate().isAfter(filter.getToDate())) {
            throw new IllegalArgumentException("fromDate cannot be after toDate.");
        }

        return DateRange.of(filter.getFromDate(), filter.getToDate());
    }


    public int resolvePage(FinancialReportFilter filter) {

        Integer page = filter.getPage();

        if (page != null && page < 0) {
            throw new IllegalArgumentException("page cannot be negative.");
        }

        return page == null ? 0 : page;
    }


    public int resolveSize(FinancialReportFilter filter) {

        Integer size = filter.getSize();

        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }

        return size;
    }


    // Predicates restricting a timestamp column to the range; empty when the
    // range is open on both ends.
    public List<Predicate> within(
            CriteriaBuilder cb,
            Expression<LocalDateTime> column,
            DateRange range) {

        List<Predicate> predicates = new ArrayList<>();

        if (range.start() != null) {
            predicates.add(cb.greaterThanOrEqualTo(column, range.start()));
        }

        if (range.endExclusive() != null) {
            predicates.add(cb.lessThan(column, range.endExclusive()));
        }

        return predicates;
    }


    // Case-insensitive "contains" match. % and _ typed by the user are matched
    // literally rather than as wildcards.
    public Predicate contains(CriteriaBuilder cb, Expression<String> column, String term) {

        String escaped = term.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        return cb.like(cb.lower(column), "%" + escaped + "%", '\\');
    }


    // Patient search, number searches and referred-by filters, applied to a bill.
    public List<Predicate> billSearchPredicates(
            CriteriaBuilder cb,
            From<?, Billing> bill,
            From<?, CustomerManagement> customer,
            From<?, DoctorDetails> doctor,
            FinancialReportFilter filter) {

        List<Predicate> predicates = new ArrayList<>();

        if (hasText(filter.getPatientSearch())) {
            predicates.add(cb.or(
                    contains(cb, customer.get("customerName"), filter.getPatientSearch()),
                    contains(cb, customer.get("customerPhoneNo"), filter.getPatientSearch())));
        }

        if (hasText(filter.getNumberSearch())) {
            predicates.add(cb.or(
                    contains(cb, customer.get("patientNumber"), filter.getNumberSearch()),
                    contains(cb, bill.get("opIpNumber"), filter.getNumberSearch()),
                    contains(cb, bill.get("billNo"), filter.getNumberSearch())));
        }

        if (hasText(filter.getPatientNo())) {
            predicates.add(contains(cb, customer.get("patientNumber"), filter.getPatientNo()));
        }

        // IP and OP numbers share one column; the bill's customer type says which.
        if (hasText(filter.getIpNo())) {
            predicates.add(contains(cb, bill.get("opIpNumber"), filter.getIpNo()));
            predicates.add(cb.equal(bill.get("customerType"), CustomerType.IP_PATIENT));
        }

        if (hasText(filter.getOpNo())) {
            predicates.add(contains(cb, bill.get("opIpNumber"), filter.getOpNo()));
            predicates.add(cb.equal(bill.get("customerType"), CustomerType.OP_PATIENT));
        }

        if (hasText(filter.getBillNo())) {
            predicates.add(contains(cb, bill.get("billNo"), filter.getBillNo()));
        }

        if (filter.getDoctorIds() != null && !filter.getDoctorIds().isEmpty()) {
            predicates.add(doctor.get("doctorId").in(filter.getDoctorIds()));
        }

        return predicates;
    }


    // Amount a bill is settled against: the rounded net amount, or the net
    // amount for older bills with no round-off stored.
    public Expression<BigDecimal> billAmount(CriteriaBuilder cb, From<?, Billing> bill) {

        return cb.coalesce(
                bill.<BigDecimal>get("totalNetAmountAfterRoundOff"),
                bill.<BigDecimal>get("totalNetAmount"));
    }


    // Product search / multi-select, HSN search / multi-select and GST %
    // multi-select, applied to a line's product.
    public List<Predicate> productPredicates(
            CriteriaBuilder cb,
            From<?, ProductDetails> product,
            FinancialReportFilter filter) {

        List<Predicate> predicates = new ArrayList<>();

        if (hasText(filter.getProductSearch())) {
            predicates.add(contains(cb, product.get("productName"), filter.getProductSearch()));
        }

        List<String> productIds = trimmed(filter.getProductIds());

        if (!productIds.isEmpty()) {
            predicates.add(product.get("productId").in(productIds));
        }

        if (hasText(filter.getHsnSearch())) {
            predicates.add(contains(cb, product.get("hsnNo"), filter.getHsnSearch()));
        }

        List<String> hsnCodes = trimmed(filter.getHsnCodes());

        if (!hsnCodes.isEmpty()) {
            predicates.add(cb.trim(product.<String>get("hsnNo")).in(hsnCodes));
        }

        List<String> gstPercentages = trimmed(filter.getGstPercentages());

        if (!gstPercentages.isEmpty()) {
            predicates.add(cb.trim(product.<String>get("gstPercentage")).in(gstPercentages));
        }

        return predicates;
    }


    /**
     * Patient type multi-select. Accepts the short labels shown on screen
     * (IP, OP, WALK_IN, DAY_CARE) as well as the stored names (IP_PATIENT,
     * OP_PATIENT, WALK_IN, DAYCARE).
     */
    public List<CustomerType> resolvePatientTypes(FinancialReportFilter filter) {

        return trimmed(filter.getPatientTypes()).stream()
                .map(value -> switch (value.toUpperCase().replace(' ', '_').replace("-", "_")) {
                    case "IP", "IP_PATIENT" -> CustomerType.IP_PATIENT;
                    case "OP", "OP_PATIENT" -> CustomerType.OP_PATIENT;
                    case "WALK_IN", "WALKIN" -> CustomerType.WALK_IN;
                    case "DAYCARE", "DAY_CARE" -> CustomerType.DAYCARE;
                    default -> throw new IllegalArgumentException(
                            "Unknown patient type: " + value + ". Use IP, OP, WALK_IN or DAY_CARE.");
                })
                .distinct()
                .toList();
    }


    // Payment mode multi-select: CASH, UPI, CARD, CREDIT.
    public List<PaymentMode> resolvePaymentModes(FinancialReportFilter filter) {

        return trimmed(filter.getPaymentModes()).stream()
                .map(value -> {
                    try {
                        return PaymentMode.valueOf(value.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                "Unknown payment mode: " + value + ". Use CASH, UPI, CARD or CREDIT.");
                    }
                })
                .distinct()
                .toList();
    }


    // Bills with at least one payment in any of the given modes.
    public Predicate paidWithAnyOf(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            From<?, Billing> bill,
            List<PaymentMode> paymentModes) {

        Subquery<Long> payments = query.subquery(Long.class);
        Root<BillingPayment> payment = payments.from(BillingPayment.class);

        payments.select(payment.get("paymentId"))
                .where(
                        cb.equal(payment.get("billing"), bill),
                        payment.get("paymentMode").in(paymentModes));

        return cb.exists(payments);
    }


    public static List<String> trimmed(List<String> values) {

        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(FinancialReportSupport::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }


    // "Generated by" on a report: the user's name, or their id if no name is set.
    public static String generatedBy(UserDetails user) {
        return hasText(user.getFullName()) ? user.getFullName() : user.getUserId();
    }


    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


    // [start, endExclusive) so the whole of the last day is included.
    public record DateRange(LocalDateTime start, LocalDateTime endExclusive) {

        static DateRange of(LocalDate from, LocalDate to) {
            return new DateRange(
                    from == null ? null : from.atStartOfDay(),
                    to == null ? null : to.plusDays(1).atStartOfDay());
        }
    }
}
