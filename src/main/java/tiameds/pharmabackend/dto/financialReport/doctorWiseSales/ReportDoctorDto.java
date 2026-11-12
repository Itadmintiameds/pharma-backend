package tiameds.pharmabackend.dto.financialReport.doctorWiseSales;

import lombok.AllArgsConstructor;
import lombok.Data;

// One doctor for the report's doctor / referred-by dropdown, with the pharmacy
// it was added in (doctors are per pharmacy, so a name can repeat).
@Data
@AllArgsConstructor
public class ReportDoctorDto {

    private Long doctorId;
    private String doctorName;
    private String pharmacyId;
    private String pharmacyName;
}
