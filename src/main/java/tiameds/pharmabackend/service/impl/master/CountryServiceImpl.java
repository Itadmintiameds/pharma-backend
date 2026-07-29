package tiameds.pharmabackend.service.impl.master;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.master.CountryDto;
import tiameds.pharmabackend.entity.master.Country;
import tiameds.pharmabackend.repository.master.CountryRepository;
import tiameds.pharmabackend.service.master.CountryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CountryDto> getAllCountries() {
        return countryRepository
                .findAll(Sort.by("countryId"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CountryDto getCountryById(Long countryId) {
        return toDto(findCountry(countryId));
    }

    @Override
    public CountryDto createCountry(CountryDto countryDto) {

        Country country = new Country();
        country.setCountryName(countryDto.getCountryName());
        country.setDialingCode(countryDto.getDialingCode());
        country.setIsActive(countryDto.getIsActive() != null ? countryDto.getIsActive() : true);
        country.setCreatedAt(LocalDateTime.now());

        return toDto(countryRepository.save(country));
    }

    @Override
    public CountryDto updateCountry(Long countryId, CountryDto countryDto) {

        Country country = findCountry(countryId);

        country.setCountryName(countryDto.getCountryName());
        country.setDialingCode(countryDto.getDialingCode());
        if (countryDto.getIsActive() != null) {
            country.setIsActive(countryDto.getIsActive());
        }
        country.setModifiedAt(LocalDateTime.now());

        return toDto(countryRepository.save(country));
    }

    @Override
    public CountryDto updateCountryStatus(Long countryId, Boolean isActive) {

        Country country = findCountry(countryId);

        country.setIsActive(isActive);
        country.setModifiedAt(LocalDateTime.now());

        return toDto(countryRepository.save(country));
    }

    private Country findCountry(Long countryId) {
        return countryRepository.findById(countryId)
                .orElseThrow(() -> new RuntimeException("Country not found with id: " + countryId));
    }

    private CountryDto toDto(Country country) {
        CountryDto dto = new CountryDto();
        dto.setCountryId(country.getCountryId());
        dto.setCountryName(country.getCountryName());
        dto.setDialingCode(country.getDialingCode());
        dto.setIsActive(country.getIsActive());
        return dto;
    }
}
