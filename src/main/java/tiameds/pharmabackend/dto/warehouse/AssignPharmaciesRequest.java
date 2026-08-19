package tiameds.pharmabackend.dto.warehouse;

import lombok.Data;

import java.util.List;

@Data
public class AssignPharmaciesRequest {

    private List<String> pharmacyIds;
}
