package tiameds.pharmabackend.service.impl.financialReport.common;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.financialReport.common.FinancialReportFilter;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;

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

        List<String> requested = filter.getPharmacyIds() == null
                ? List.of()
                : filter.getPharmacyIds().stream()
                        .filter(FinancialReportSupport::hasText)
                        .map(String::trim)
                        .distinct()
                        .toList();

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
