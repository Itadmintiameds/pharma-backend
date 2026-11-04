package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// One page of the Collection Transaction Details table.
@Data
@JsonPropertyOrder({"data", "displayedTotal", "page", "size", "totalRecords", "totalPages"})
public class UserWiseCollectionReportPageDto {

    private List<UserWiseCollectionReportRowDto> data;

    // "Displayed Transactions Total": collected amount of this page only.
    private BigDecimal displayedTotal;

    // 0-based; offset = page * size.
    private int page;
    private int size;
    private long totalRecords;
    private int totalPages;
}
