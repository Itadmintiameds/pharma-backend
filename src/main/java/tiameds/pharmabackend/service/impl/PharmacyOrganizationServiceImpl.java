package tiameds.pharmabackend.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.dto.PharmacyOrganizationDto;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.mapper.PharmacyOrganizationMapper;
import tiameds.pharmabackend.repository.PharmacyOrganizationRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.PharmacyOrganizationService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PharmacyOrganizationServiceImpl implements PharmacyOrganizationService {

    private final PharmacyOrganizationRepository organizationRepository;
    private final PharmacyOrganizationMapper organizationMapper;
    private final UserDetailsRepository userDetailsRepository;

    @Override
    public PharmacyOrganizationDto createOrganization(
            PharmacyOrganizationDto organizationDto,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PharmacyOrganization organization =
                organizationMapper.toEntity(organizationDto);

        organization.setCreatedAt(LocalDateTime.now());
        organization.setIsRejected(Boolean.FALSE);
        organization.setIsActive(Boolean.TRUE);

        PharmacyOrganization savedOrganization =
                organizationRepository.save(organization);

        // Associate the logged-in user with this organization
        persistentUser.setOrganization(savedOrganization);

        userDetailsRepository.save(persistentUser);

        return organizationMapper.toDto(savedOrganization);
    }

    @Override
    public void rejectRequest(Long userId) {

        UserDetails user = userDetailsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PharmacyOrganization organization = user.getOrganization();

        boolean userHasPharmacy =
                user.getPharmacies() != null && !user.getPharmacies().isEmpty();

        boolean organizationHasPharmacy =
                organization != null
                        && organization.getPharmacies() != null
                        && !organization.getPharmacies().isEmpty();

        // A pharmacy already exists for this user/organization: they remain active,
        // only the individual registration is rejected on the admin side.
        if (userHasPharmacy || organizationHasPharmacy) {
            log.info("Skipping reject for user {}: user/organization already has pharmacies", userId);
            return;
        }

        user.setIsRejected(Boolean.TRUE);
        user.setModifiedAt(LocalDateTime.now());

        userDetailsRepository.save(user);

        if (organization != null) {
            organization.setIsRejected(Boolean.TRUE);
            organizationRepository.save(organization);
        }
    }
}