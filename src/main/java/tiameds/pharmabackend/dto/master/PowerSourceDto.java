package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class PowerSourceDto {
    private Long powerSourceId;
    private String powerSourceName;
    private Boolean isActive;
}
