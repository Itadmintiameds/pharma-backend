package tiameds.pharmabackend.dto.auditReports;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

// Report header details of one pharmacy in the report.
@Data
@JsonPropertyOrder({"pharmacyId", "pharmacyName", "address", "phoneNumber", "emailAddress", "gstin"})
public class GrossProfitSalesBookPharmacyDto {

    private String pharmacyId;
    private String pharmacyName;
    private String address;
    private String phoneNumber;
    private String emailAddress;
    private String gstin;
}
