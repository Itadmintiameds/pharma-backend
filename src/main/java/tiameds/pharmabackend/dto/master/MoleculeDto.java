package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class MoleculeDto {
    private Long moleculeId;
    private String moleculeName;
    private String drugSchedule;
    private Boolean isActive;
}
