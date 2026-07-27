package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class IntendedUseAreaDto {
    private Long intendedUseAreaId;
    private String intendedUseAreaName;
    private Boolean isActive;
}
