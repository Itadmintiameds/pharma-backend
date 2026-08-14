package tiameds.pharmabackend.service.audit;

import tiameds.pharmabackend.dto.audit.AuditActorDto;
import tiameds.pharmabackend.dto.audit.UserAuditPageDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.time.LocalDate;
import java.util.List;

public interface UserAuditLogService {

    UserAuditPageDto getUserAuditLogs(
            LocalDate fromDate,
            LocalDate toDate,
            String actorUserId,
            String action,
            String cursor,
            Integer size,
            UserDetails user);

    List<AuditActorDto> getAuditActors(UserDetails user);

    List<String> getAuditActions();
}
