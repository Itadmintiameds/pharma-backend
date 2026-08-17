package tiameds.pharmabackend.controller.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.audit.AuditActorDto;
import tiameds.pharmabackend.dto.audit.UserAuditPageDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.audit.UserAuditLogService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class UserAuditLogController {

    private final UserAuditLogService userAuditLogService;

    @GetMapping("/user-logs")
    public ResponseEntity<?> getUserAuditLogs(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) String actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserAuditPageDto page = userAuditLogService.getUserAuditLogs(
                fromDate,
                toDate,
                actorUserId,
                action,
                cursor,
                size,
                currentUser.getUser());

        return ResponseEntity.ok(page);
    }


    /**
     * Activity timeline for a single user: by default everything they did plus
     * everything that was done to them. Narrow with scope=ACTOR or scope=TARGET.
     */
    @GetMapping("/user-logs/user/{userId}")
    public ResponseEntity<?> getAuditLogsForUser(
            @PathVariable String userId,

            @RequestParam(required = false) String scope,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @RequestParam(required = false) String action,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,

            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserAuditPageDto page = userAuditLogService.getAuditLogsForUser(
                userId,
                scope,
                fromDate,
                toDate,
                action,
                cursor,
                size,
                currentUser.getUser());

        return ResponseEntity.ok(page);
    }


    /** Values for the "Users" filter dropdown. */
    @GetMapping("/user-logs/actors")
    public ResponseEntity<?> getAuditActors(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<AuditActorDto> actors =
                userAuditLogService.getAuditActors(currentUser.getUser());

        return ResponseEntity.ok(actors);
    }


    /** Values for the "Actions" filter dropdown. */
    @GetMapping("/user-logs/actions")
    public ResponseEntity<?> getAuditActions(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(userAuditLogService.getAuditActions());
    }
}
