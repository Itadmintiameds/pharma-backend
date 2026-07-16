package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.dto.UserSummaryDto;
import tiameds.pharmabackend.entity.PharmaRoles;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.mapper.UserDetailsMapper;
import tiameds.pharmabackend.repository.PharmaRolesRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.UserDetailsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDetailsRepository userDetailsRepository;
    private final PharmaRolesRepository pharmaRolesRepository;
    private final UserDetailsMapper userDetailsMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetailsDto registerUser(UserDetailsDto userDetailsDto) {
        Long roleId = 1L;

        if (userDetailsDto.getPharmaRolesDto() != null
                && userDetailsDto.getPharmaRolesDto().getRoleId() != null) {
            roleId = userDetailsDto.getPharmaRolesDto().getRoleId();
        }

        PharmaRoles role = pharmaRolesRepository
                .findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        UserDetails user = userDetailsMapper.toEntity(userDetailsDto, role);

        user.setPassword(passwordEncoder.encode(userDetailsDto.getPassword()));

        user.setCreatedAt(LocalDateTime.now());
        user.setIsRejected(Boolean.FALSE);
        user.setUserStatus("Active");

        UserDetails savedUser = userDetailsRepository.save(user);

        return userDetailsMapper.toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllUsers(Long currentUserId) {

        Long organizationId = getOrganizationIdOfUser(currentUserId);

        return userDetailsRepository
                .findAllByOrganizationIdWithPharmacies(organizationId)
                .stream()
                .map(userDetailsMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailsDto getUserById(Long currentUserId, Long userId) {

        Long organizationId = getOrganizationIdOfUser(currentUserId);

        UserDetails user = userDetailsRepository
                .findByUserIdWithOrganization(userId)
                .filter(u -> u.getOrganization() != null
                        && organizationId.equals(u.getOrganization().getOrganizationId()))
                .orElseThrow(() ->
                        new RuntimeException("User not found in your organization with id : " + userId));

        return userDetailsMapper.toDto(user);
    }

    private Long getOrganizationIdOfUser(Long userId) {

        UserDetails user = userDetailsRepository
                .findByUserIdWithOrganization(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id : " + userId));

        if (user.getOrganization() == null) {
            throw new RuntimeException("User is not associated with any organization");
        }

        return user.getOrganization().getOrganizationId();
    }

//    @Override
//    public void deleteUserByPharmacyRegistrationId(String pharmacyRegistrationId) {
//
//        UserDetails user = userDetailsRepository
//                .findByPharmacyRegistrationId(pharmacyRegistrationId)
//                .orElseThrow(() ->
//                        new RuntimeException("User not found with Pharmacy Registration Id : "
//                                + pharmacyRegistrationId));
//
//        userDetailsRepository.delete(user);
//    }
}