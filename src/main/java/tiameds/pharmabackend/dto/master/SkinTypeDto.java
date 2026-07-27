package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class SkinTypeDto {
    private Long skinTypeId;
    private String skinTypeName;
    private Boolean isActive;
}
