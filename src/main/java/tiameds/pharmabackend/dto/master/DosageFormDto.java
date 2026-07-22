package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class DosageFormDto {
    private Long dosageId;
    private String dosageName;
    private Boolean isActive;
}
