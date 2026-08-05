package tiameds.pharmabackend.dto.billing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrescriptionUploadDto {

    private Long billingId;
    private String prescriptionUrl;
}