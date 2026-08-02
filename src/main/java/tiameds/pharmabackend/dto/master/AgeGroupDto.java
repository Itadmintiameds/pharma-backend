package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class AgeGroupDto {
    private Long ageGroupId;
    private String ageGroupName;
    private Boolean isActive;
}
