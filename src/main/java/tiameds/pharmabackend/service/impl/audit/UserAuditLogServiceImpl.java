package tiameds.pharmabackend.service.impl.audit;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.audit.AuditActorDto;
import tiameds.pharmabackend.dto.audit.UserAuditLogDto;
import tiameds.pharmabackend.dto.audit.UserAuditPageDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.audit.UserAuditLog;
import tiameds.pharmabackend.enums.UserAuditAction;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.audit.UserAuditLogRepository;
import tiameds.pharmabackend.service.audit.UserAuditLogService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAuditLogServiceImpl implements UserAuditLogService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private static final String SCOPE_ACTOR = "ACTOR";
    private static final String SCOPE_TARGET = "TARGET";
    private static final String SCOPE_ALL = "ALL";

    private final UserAuditLogRepository userAuditLogRepository;
    private final UserDetailsRepository userDetailsRepository;


    @Override
    public UserAuditPageDto getUserAuditLogs(
            LocalDate fromDate,
            LocalDate toDate,
            String actorUserId,
            String action,
            String cursor,
            Integer size,
            UserDetails user) {

        return loadPage(
                requireOrganizationId(user),
                fromDate,
                toDate,
                (actorUserId != null && !actorUserId.isBlank()) ? actorUserId : null,
                null,
                null,
                action,
                cursor,
                size);
    }


    @Override
    public UserAuditPageDto getAuditLogsForUser(
            String userId,
            String scope,
            LocalDate fromDate,
            LocalDate toDate,
            String action,
            String cursor,
            Integer size,
            UserDetails user) {

        Long organizationId = requireOrganizationId(user);

        // A caller may only read the timeline of someone in their organization.
        requireUserInOrganization(userId, organizationId);

        return loadPage(
                organizationId,
                fromDate,
                toDate,
                null,
                userId,
                normalizeScope(scope),
                action,
                cursor,
                size);
    }


    private UserAuditPageDto loadPage(
            Long organizationId,
            LocalDate fromDate,
            LocalDate toDate,
            String actorUserId,
            String subjectUserId,
            String scope,
            String action,
            String cursor,
            Integer size) {

        int pageSize = resolvePageSize(size);

        UserAuditAction auditAction = parseAction(action);

        // A date range is inclusive of both days as the user sees them.
        LocalDateTime from = fromDate != null
                ? fromDate.atStartOfDay()
                : null;

        LocalDateTime to = toDate != null
                ? toDate.atTime(LocalTime.MAX)
                : null;

        LocalDateTime cursorCreatedAt = null;
        Long cursorAuditId = null;

        if (cursor != null && !cursor.isBlank()) {

            String[] parts = cursor.split("_");

            if (parts.length != 2) {
                throw new RuntimeException("Invalid cursor: " + cursor);
            }

            try {
                cursorCreatedAt = LocalDateTime.parse(parts[0]);
                cursorAuditId = Long.parseLong(parts[1]);

            } catch (Exception e) {
                throw new RuntimeException("Invalid cursor: " + cursor);
            }
        }

        Specification<UserAuditLog> spec = buildSpecification(
                organizationId,
                from,
                to,
                actorUserId,
                subjectUserId,
                scope,
                auditAction,
                cursorCreatedAt,
                cursorAuditId);

        // One extra row tells us whether another page exists without a count query.
        List<UserAuditLog> rows = userAuditLogRepository.findAll(
                spec,
                PageRequest.of(
                        0,
                        pageSize + 1,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("auditId"))))
                .getContent();

        boolean hasMore = rows.size() > pageSize;

        List<UserAuditLog> pageRows = hasMore
                ? rows.subList(0, pageSize)
                : rows;

        List<UserAuditLogDto> data = pageRows.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        String nextCursor = null;

        if (hasMore && !pageRows.isEmpty()) {

            UserAuditLog last = pageRows.get(pageRows.size() - 1);

            nextCursor = last.getCreatedAt() + "_" + last.getAuditId();
        }

        return new UserAuditPageDto(data, nextCursor, hasMore);
    }


    @Override
    public List<AuditActorDto> getAuditActors(UserDetails user) {

        Long organizationId = requireOrganizationId(user);

        List<AuditActorDto> actors = new ArrayList<>();

        for (Object[] row : userAuditLogRepository.findDistinctActors(organizationId)) {
            actors.add(new AuditActorDto(
                    (String) row[0],
                    (String) row[1]));
        }

        return actors;
    }


    @Override
    public List<String> getAuditActions() {

        return Arrays.stream(UserAuditAction.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }


    /**
     * Only the filters actually supplied become predicates, so no untyped null
     * parameter ever reaches PostgreSQL.
     */
    private Specification<UserAuditLog> buildSpecification(
            Long organizationId,
            LocalDateTime from,
            LocalDateTime to,
            String actorUserId,
            String subjectUserId,
            String scope,
            UserAuditAction action,
            LocalDateTime cursorCreatedAt,
            Long cursorAuditId) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            if (actorUserId != null) {
                predicates.add(cb.equal(root.get("actorUserId"), actorUserId));
            }

            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            // Single-user timeline: what they did, what was done to them, or both.
            if (subjectUserId != null) {

                if (SCOPE_ACTOR.equals(scope)) {
                    predicates.add(cb.equal(root.get("actorUserId"), subjectUserId));

                } else if (SCOPE_TARGET.equals(scope)) {
                    predicates.add(cb.equal(root.get("targetUserId"), subjectUserId));

                } else {
                    predicates.add(
                            cb.or(
                                    cb.equal(root.get("actorUserId"), subjectUserId),
                                    cb.equal(root.get("targetUserId"), subjectUserId)));
                }
            }

            // Keyset cursor: everything strictly older than the last row seen,
            // with auditId breaking ties on identical timestamps.
            if (cursorCreatedAt != null && cursorAuditId != null) {
                predicates.add(
                        cb.or(
                                cb.lessThan(root.get("createdAt"), cursorCreatedAt),
                                cb.and(
                                        cb.equal(root.get("createdAt"), cursorCreatedAt),
                                        cb.lessThan(root.get("auditId"), cursorAuditId))));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    private UserAuditLogDto toDto(UserAuditLog entity) {

        UserAuditLogDto dto = new UserAuditLogDto();

        dto.setAuditId(entity.getAuditId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setAction(entity.getAction());
        dto.setActorUserId(entity.getActorUserId());
        dto.setActorName(entity.getActorName());
        dto.setTargetUserId(entity.getTargetUserId());
        dto.setTargetName(entity.getTargetName());
        dto.setDetails(entity.getDetails());
        dto.setIpAddress(entity.getIpAddress());
        dto.setPharmacyId(entity.getPharmacyId());

        return dto;
    }


    /**
     * The log is organization scoped, so the caller only ever sees activity from
     * their own organization.
     */
    private Long requireOrganizationId(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (persistentUser.getOrganization() == null) {
            throw new RuntimeException("User is not linked to an organization");
        }

        return persistentUser.getOrganization().getOrganizationId();
    }


    private void requireUserInOrganization(String userId, Long organizationId) {

        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("User id is required");
        }

        UserDetails target = userDetailsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found : " + userId));

        if (target.getOrganization() == null
                || !organizationId.equals(target.getOrganization().getOrganizationId())) {

            throw new RuntimeException("User not found in your organization : " + userId);
        }
    }


    private String normalizeScope(String scope) {

        if (scope == null || scope.isBlank()) {
            return SCOPE_ALL;
        }

        String normalized = scope.trim().toUpperCase();

        if (SCOPE_ACTOR.equals(normalized)
                || SCOPE_TARGET.equals(normalized)
                || SCOPE_ALL.equals(normalized)) {
            return normalized;
        }

        throw new RuntimeException(
                "Invalid scope : " + scope + ". Allowed values are ACTOR, TARGET, ALL");
    }


    private int resolvePageSize(Integer size) {

        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }


    private UserAuditAction parseAction(String action) {

        if (action == null || action.isBlank()) {
            return null;
        }

        try {
            return UserAuditAction.valueOf(action.trim().toUpperCase());

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown action: " + action);
        }
    }
}
