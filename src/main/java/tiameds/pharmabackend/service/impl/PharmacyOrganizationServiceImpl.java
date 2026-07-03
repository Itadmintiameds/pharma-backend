package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.PharmacyOrganizationDto;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.mapper.PharmacyOrganizationMapper;
import tiameds.pharmabackend.repository.PharmacyOrganizationRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.PharmacyOrganizationService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PharmacyOrganizationServiceImpl implements PharmacyOrganizationService {

    private final PharmacyOrganizationRepository organizationRepository;
    private final PharmacyOrganizationMapper organizationMapper;
    private final UserDetailsRepository userRepository;

    @Override
    public PharmacyOrganizationDto createOrganization(PharmacyOrganizationDto dto) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

//        UserDetails user = userRepository.findByUserName(username)
//                .orElseThrow(() ->
//                        new RuntimeException("Logged in user not found"));

        PharmacyOrganization organization =
                organizationMapper.toEntity(dto);

        organization.setCreatedAt(LocalDateTime.now());
        organization.setIsActive(true);
        organization.setIsRejected(false);

        PharmacyOrganization savedOrganization =
                organizationRepository.save(organization);

        // Update logged-in user
//        user.setOrganization(savedOrganization);
//
//        userRepository.save(user);

        return organizationMapper.toDto(savedOrganization);
    }
}