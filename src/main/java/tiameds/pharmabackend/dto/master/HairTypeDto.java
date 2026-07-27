package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class HairTypeDto {
    private Long hairTypeId;
    private String hairTypeName;
    private Boolean isActive;
}
