package tiameds.pharmabackend.dto.financialReport.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// One page of report rows. page is zero-based, matching the request.
@Data
@AllArgsConstructor
public class ReportPageDto<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
