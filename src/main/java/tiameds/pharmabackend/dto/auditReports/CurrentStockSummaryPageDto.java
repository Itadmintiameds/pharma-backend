package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

// One page of the Stock Details table.
@Data
@JsonPropertyOrder({"data", "page", "size", "totalRecords", "totalPages"})
public class CurrentStockSummaryPageDto {

    private List<CurrentStockSummaryRowDto> data;

    // 0-based; offset = page * size.
    private int page;
    private int size;
    private long totalRecords;
    private int totalPages;
}
