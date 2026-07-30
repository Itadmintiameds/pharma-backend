package tiameds.pharmabackend.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoggedInUserPharmacyDto {

    private String pharmacyId;
    private String pharmacyName;
}
