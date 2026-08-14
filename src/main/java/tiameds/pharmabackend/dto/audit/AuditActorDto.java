package tiameds.pharmabackend.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Row for the "Users" filter dropdown. */
@Data
@AllArgsConstructor
public class AuditActorDto {

    private String userId;
    private String userName;
}
