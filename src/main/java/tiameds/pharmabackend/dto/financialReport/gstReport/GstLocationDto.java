package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.AllArgsConstructor;
import lombok.Data;

// A pharmacy or warehouse covered by the report.
@Data
@AllArgsConstructor
public class GstLocationDto {

    // PHARMACY or WAREHOUSE.
    private String locationType;

    private String locationId;

    private String locationName;

    private String address;
}
