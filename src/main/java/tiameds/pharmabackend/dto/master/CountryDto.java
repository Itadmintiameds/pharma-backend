package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class CountryDto {
    private Long countryId;
    private String countryName;
    private String dialingCode;
    private Boolean isActive;
}
