package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

// One page of the Product-wise Analysis tables.
@Data
@JsonPropertyOrder({"data", "page", "size", "totalRecords", "totalPages"})
public class ProductWiseReportPageDto {

    private List<ProductWiseReportRowDto> data;

    // 0-based; offset = page * size.
    private int page;
    private int size;
    private long totalRecords;
    private int totalPages;
}
