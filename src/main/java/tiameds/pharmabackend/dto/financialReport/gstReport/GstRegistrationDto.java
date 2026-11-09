package tiameds.pharmabackend.dto.financialReport.gstReport;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// One GSTIN in the report header: the registered business and the selected
// locations filing under it.
@Data
@AllArgsConstructor
public class GstRegistrationDto {

    // The pharmacy's GST number, or the organization's when the pharmacy has
    // none (and always for a warehouse). Null when neither is set.
    private String gstin;

    // The organization's name, or the location's when there is none.
    private String dealerName;

    // Address of the first location under this GSTIN.
    private String address;

    private List<GstLocationDto> locations;
}
