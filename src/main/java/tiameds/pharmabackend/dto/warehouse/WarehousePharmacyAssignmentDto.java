package tiameds.pharmabackend.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WarehousePharmacyAssignmentDto {

    private String warehouseId;
    private List<String> pharmacyIds;
}
