package tiameds.pharmabackend.dto.billing;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DoctorDetailsDto {

    private Long doctorId;
    private String pharmacyId;
    private String doctorName;
    private String createdBy;
    private LocalDateTime createdAt;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
}
