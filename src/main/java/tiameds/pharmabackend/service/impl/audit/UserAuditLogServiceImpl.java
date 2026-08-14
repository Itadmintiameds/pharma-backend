package tiameds.pharmabackend.service.impl.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

        Long organizationId = requireOrganizationId(user);

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

        // One extra row tells us whether another page exists without a count query.
        List<UserAuditLog> rows = userAuditLogRepository.findPage(
                organizationId,
                from,
                to,
                (actorUserId != null && !actorUserId.isBlank()) ? actorUserId : null,
                auditAction,
                cursorCreatedAt,
                cursorAuditId,
                PageRequest.of(0, pageSize + 1));

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
