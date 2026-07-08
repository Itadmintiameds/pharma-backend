package tiameds.pharmabackend.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.mapper.PharmacyDetailsMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.PharmacyOrganizationRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.PharmacyDetailsService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PharmacyDetailsServiceImpl implements PharmacyDetailsService {

    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final PharmacyDetailsMapper pharmacyDetailsMapper;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyOrganizationRepository pharmacyOrganizationRepository;


    @Override
    public PharmacyDetailsDto createPharmacy(PharmacyDetailsDto pharmacyDetailsDto) {

        UserDetails persistentUser = userDetailsRepository.findById(pharmacyDetailsDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PharmacyOrganization pharmacyOrganization = pharmacyOrganizationRepository.findById(persistentUser.getOrganization().getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Pharmacy Organization not found"));

        PharmacyDetails pharmacy =
                pharmacyDetailsMapper.toEntity(pharmacyDetailsDto);

        pharmacy.setPharmacyId(generatePharmacyId(
                pharmacy.getPharmacyName(),
                pharmacy.getPharmacyType()));

        pharmacy.setCreatedAt(LocalDateTime.now());
        pharmacy.setCreatedBy(persistentUser.getUserEmail());

        pharmacy.getUsers().add(persistentUser);
        persistentUser.getPharmacies().add(pharmacy);

        pharmacy.setOrganization(pharmacyOrganization);

        if (pharmacy.getDocuments() != null) {
            pharmacy.getDocuments().forEach(document ->
                    document.setPharmacy(pharmacy));
        }

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