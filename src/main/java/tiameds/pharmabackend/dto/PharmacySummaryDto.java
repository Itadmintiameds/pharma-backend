package tiameds.pharmabackend.dto;

import lombok.Data;

@Data
public class PharmacySummaryDto {

    private String pharmacyId;
    private String pharmacyName;
    private String pharmacyBranch;
    private String pharmacyCity;
}
