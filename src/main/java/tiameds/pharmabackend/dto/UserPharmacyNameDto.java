package tiameds.pharmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// A user of the organization with the pharmacies they are mapped to, for
// user dropdowns (e.g. the billing user filter on reports).
@Data
@AllArgsConstructor
public class UserPharmacyNameDto {

    private String userId;
    private String userName;
    private List<String> pharmacyIds;
}
