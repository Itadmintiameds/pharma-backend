package tiameds.pharmabackend.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.dto.LoggedInUserPharmacyDto;
import tiameds.pharmabackend.dto.PharmacyDetailsDto;
import tiameds.pharmabackend.dto.PharmacySummaryDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.mapper.PharmacyDetailsMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.PharmacyOrganizationRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.PharmacyDetailsService;
import tiameds.pharmabackend.service.S3Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PharmacyDetailsServiceImpl implements PharmacyDetailsService {

    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final PharmacyDetailsMapper pharmacyDetailsMapper;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyOrganizationRepository pharmacyOrganizationRepository;
    private final S3Service s3Service;

    private static final DateTimeFormatter DOCUMENT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");


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
            pharmacy.getDocuments().forEach(document -> {
                document.setPharmacy(pharmacy);

                if (document.getDocumentUrl() != null
                        && !document.getDocumentUrl().isBlank()) {

                    String targetKey = buildDocumentKey(
                            pharmacy.getPharmacyId(),
                            document.getDocumentType(),
                            document.getDocumentUrl());

                    String newUrl = s3Service.copyFromExternalUrl(
                            document.getDocumentUrl(), targetKey);

                    document.setDocumentUrl(newUrl);
                }
            });
        }

        PharmacyDetails savedPharmacy =
                pharmacyDetailsRepository.save(pharmacy);

        return pharmacyDetailsMapper.toDto(savedPharmacy);
    }

    @Override
    public List<PharmacySummaryDto> getPharmacyCitiesOfTheOrganization(Long currentUserId) {

        UserDetails currentUser = userDetailsRepository
                .findByUserIdWithOrganization(currentUserId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id : " + currentUserId));

        if (currentUser.getOrganization() == null) {
            throw new RuntimeException("User is not associated with any organization");
        }

        return pharmacyDetailsRepository
                .findAllByOrganization_OrganizationIdOrderByPharmacyCity(
                        currentUser.getOrganization().getOrganizationId())
                .stream()
                .map(pharmacy -> {
                    PharmacySummaryDto dto = new PharmacySummaryDto();
                    dto.setPharmacyId(pharmacy.getPharmacyId());
                    dto.setPharmacyName(pharmacy.getPharmacyName());
                    dto.setPharmacyBranch(pharmacy.getPharmacyBranch());
                    dto.setPharmacyCity(pharmacy.getPharmacyCity());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private String buildDocumentKey(String pharmacyId,
                                    String documentType,
                                    String sourceUrl) {

        String type = (documentType == null || documentType.isBlank())
                ? "DOCUMENT"
                : documentType.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");

        String timestamp = LocalDateTime.now().format(DOCUMENT_TIMESTAMP_FORMAT);

        String extension = "";
        String path = URI.create(sourceUrl.trim()).getPath();
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > path.lastIndexOf('/')) {
            extension = path.substring(dotIndex).toLowerCase();
        }

        return "pharmacy/" + pharmacyId + "/documents/"
                + type + "_" + timestamp + extension;
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


    @Override
    public List<LoggedInUserPharmacyDto> getLoggedInUserPharmacies(Long userId) {

        UserDetails persistentUser = userDetailsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return persistentUser.getPharmacies()
                .stream()
                .map(this::mapToDto)
                .toList();
    }



    private LoggedInUserPharmacyDto mapToDto(PharmacyDetails pharmacy) {

        return new LoggedInUserPharmacyDto(
                pharmacy.getPharmacyId(),
                pharmacy.getPharmacyName()
        );
    }
}