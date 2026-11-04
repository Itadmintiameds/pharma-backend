package tiameds.pharmabackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * One page of the organization directory. page is zero-based.
 */
@Data
@AllArgsConstructor
public class OrganizationDirectoryPageDto {

    private List<OrganizationDirectoryDto> data;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasMore;
}
