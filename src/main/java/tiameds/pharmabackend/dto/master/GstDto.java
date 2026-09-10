package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class GstDto {

    private Long gstId;
    private String gstPercentage;
    private Boolean isActive;

}
