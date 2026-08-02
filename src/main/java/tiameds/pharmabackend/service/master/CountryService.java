package tiameds.pharmabackend.service.master;

import tiameds.pharmabackend.dto.master.CountryDto;

import java.util.List;

public interface CountryService {

    List<CountryDto> getAllCountries();

    CountryDto getCountryById(Long countryId);

    CountryDto createCountry(CountryDto countryDto);

    CountryDto updateCountry(Long countryId, CountryDto countryDto);

    CountryDto updateCountryStatus(Long countryId, Boolean isActive);
}
