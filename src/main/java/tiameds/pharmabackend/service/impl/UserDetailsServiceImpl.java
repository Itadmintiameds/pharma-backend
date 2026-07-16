package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.CreateUserRequestDto;
import tiameds.pharmabackend.dto.CreateUserResponseDto;
import tiameds.pharmabackend.dto.FeaturePermissionsDto;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.dto.UserSummaryDto;
import tiameds.pharmabackend.entity.PharmaFeature;
import tiameds.pharmabackend.entity.PharmaPermission;
import tiameds.pharmabackend.entity.PharmaRoles;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.UserFeaturePermission;
import tiameds.pharmabackend.mapper.UserDetailsMapper;
import tiameds.pharmabackend.repository.PharmaFeatureRepository;
import tiameds.pharmabackend.repository.PharmaPermissionRepository;
import tiameds.pharmabackend.repository.PharmaRolesRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.UserDetailsService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDetailsRepository userDetailsRepository;
    private final PharmaRolesRepository pharmaRolesRepository;
    private final PharmaFeatureRepository pharmaFeatureRepository;
    private final PharmaPermissionRepository pharmaPermissionRepository;
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
    public CreateUserResponseDto createUserWithPermissions(
            Long currentUserId,
            CreateUserRequestDto request) {

        if (request.getUser() == null) {
            throw new RuntimeException("User details are required");
        }

        UserDetails currentUser = userDetailsRepository
                .findByUserIdWithOrganization(currentUserId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id : " + currentUserId));

        if (currentUser.getOrganization() == null) {
            throw new RuntimeException("User is not associated with any organization");
        }

        UserDetailsDto userDto = request.getUser();

        if (userDto.getUserEmail() == null || userDto.getUserEmail().isBlank()) {
            throw new RuntimeException("User email is required");
        }

        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new RuntimeException("Password is required");
        }

        if (userDetailsRepository.existsByUserEmail(userDto.getUserEmail())) {
            throw new RuntimeException(
                    "User already exists with email : " + userDto.getUserEmail());
        }

        if (userDto.getPharmaRolesDto() == null
                || userDto.getPharmaRolesDto().getRoleId() == null) {
            throw new RuntimeException("Role is required");
        }

        Long roleId = userDto.getPharmaRolesDto().getRoleId();

        PharmaRoles role = pharmaRolesRepository
                .findById(roleId)
                .orElseThrow(() ->
                        new RuntimeException("Role not found with id : " + roleId));

        UserDetails user = userDetailsMapper.toEntity(userDto, role);

        user.setUserId(null);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setOrganization(currentUser.getOrganization());
        user.setCreatedAt(LocalDateTime.now());
        user.setIsRejected(Boolean.FALSE);
        user.setUserStatus("Active");

        List<FeaturePermissionsDto> grantedPermissions =
                attachPermissions(user, request.getPermissions());

        UserDetails savedUser = userDetailsRepository.save(user);

        return new CreateUserResponseDto(
                userDetailsMapper.toDto(savedUser),
                grantedPermissions);
    }

    private List<FeaturePermissionsDto> attachPermissions(
            UserDetails user,
            List<FeaturePermissionsDto> permissions) {

        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }

        Map<Long, Set<Long>> merged = new LinkedHashMap<>();

        for (FeaturePermissionsDto featurePermissions : permissions) {

            if (featurePermissions.getPermissionIds() == null
                    || featurePermissions.getPermissionIds().isEmpty()) {
                continue;
            }

            Set<Long> permissionIds = merged.computeIfAbsent(
                    featurePermissions.getFeatureId(),
                    featureId -> new LinkedHashSet<>());

            for (Long permissionId : featurePermissions.getPermissionIds()) {
                if (permissionId != null) {
                    permissionIds.add(permissionId);
                }
            }
        }

        merged.values().removeIf(Set::isEmpty);

        if (merged.isEmpty()) {
            return List.of();
        }

        Map<Long, PharmaFeature> features = pharmaFeatureRepository
                .findAllById(merged.keySet())
                .stream()
                .collect(Collectors.toMap(
                        PharmaFeature::getFeatureId,
                        Function.identity()));

        Set<Long> allPermissionIds = merged.values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Map<Long, PharmaPermission> permissionsById = pharmaPermissionRepository
                .findAllById(allPermissionIds)
                .stream()
                .collect(Collectors.toMap(
                        PharmaPermission::getPermissionId,
                        Function.identity()));

        for (Long permissionId : allPermissionIds) {
            if (!permissionsById.containsKey(permissionId)) {
                throw new RuntimeException(
                        "Permission not found with id : " + permissionId);
            }
        }

        List<FeaturePermissionsDto> granted = new ArrayList<>();

        for (Map.Entry<Long, Set<Long>> entry : merged.entrySet()) {

            PharmaFeature feature = features.get(entry.getKey());

            if (feature == null) {
                throw new RuntimeException("Feature not found with id : " + entry.getKey());
            }

            for (Long permissionId : entry.getValue()) {

                UserFeaturePermission row = new UserFeaturePermission();
                row.setUser(user);
                row.setFeature(feature);
                row.setPermission(permissionsById.get(permissionId));

                user.getFeaturePermissions().add(row);
            }

            FeaturePermissionsDto grantedFeature = new FeaturePermissionsDto();
            grantedFeature.setFeatureId(feature.getFeatureId());
            grantedFeature.setPermissionIds(new ArrayList<>(entry.getValue()));

            granted.add(grantedFeature);
        }

        return granted;
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