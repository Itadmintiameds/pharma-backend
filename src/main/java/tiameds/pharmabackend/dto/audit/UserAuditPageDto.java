package tiameds.pharmabackend.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * One page of audit rows plus the cursor to fetch the next page.
 * nextCursor is null when there is nothing more to load.
 */
@Data
@AllArgsConstructor
public class UserAuditPageDto {

    private List<UserAuditLogDto> data;
    private String nextCursor;
    private boolean hasMore;
}
