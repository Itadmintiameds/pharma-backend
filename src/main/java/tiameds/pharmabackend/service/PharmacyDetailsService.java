package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.entity.UserDetails;

public interface PharmacyDetailsService {

    PharmacyDetailsDto createPharmacy(PharmacyDetailsDto pharmacyDetailsDto);

}
