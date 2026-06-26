package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.mapper.PharmacyDetailsMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.service.PharmacyDetailsService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PharmacyDetailsServiceImpl
        implements PharmacyDetailsService {

    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final PharmacyDetailsMapper pharmacyDetailsMapper;

    @Override
    public PharmacyDetailsDto createPharmacy(
            PharmacyDetailsDto pharmacyDetailsDto) {

        PharmacyDetails pharmacy =
                pharmacyDetailsMapper.toEntity(pharmacyDetailsDto);

        String pharmacyId = generatePharmacyId(
                pharmacy.getPharmacyName(),
                pharmacy.getPharmacyType());

        pharmacy.setPharmacyId(pharmacyId);
        pharmacy.setCreatedAt(LocalDateTime.now());

        PharmacyDetails savedPharmacy =
                pharmacyDetailsRepository.save(pharmacy);

        return pharmacyDetailsMapper.toDto(savedPharmacy);
    }


    private String generatePharmacyId(String pharmacyName,
                                      String pharmacyType) {

        String namePrefix = pharmacyName
                .trim()
                .toUpperCase()
                .substring(0, Math.min(2, pharmacyName.length()));

        String typePrefix = pharmacyType
                .trim()
                .toUpperCase()
                .substring(0, Math.min(3, pharmacyType.length()));

        String prefix = namePrefix + typePrefix;

        String latestId = pharmacyDetailsRepository.findLatestPharmacyId();

        int nextSequence = 1;

        if (latestId != null && !latestId.isBlank()) {

            String numericPart =
                    latestId.replaceAll("[^0-9]", "");

            nextSequence = Integer.parseInt(numericPart) + 1;
        }

        return prefix + String.format("%04d", nextSequence);
    }
}