package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.audit.UserAuditRecorder;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.enums.UserAuditAction;
import tiameds.pharmabackend.dto.AssignPermissionsRequestDto;
import tiameds.pharmabackend.dto.CreateUserRequestDto;
import tiameds.pharmabackend.dto.CreateUserResponseDto;
import tiameds.pharmabackend.dto.CurrentUserPermissionsDto;
import tiameds.pharmabackend.dto.FeaturePermissionsDto;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.dto.UserImageDto;
import tiameds.pharmabackend.dto.UserStatusDto;
import tiameds.pharmabackend.dto.UserSummaryDto;
import tiameds.pharmabackend.entity.PharmaFeature;
import tiameds.pharmabackend.entity.PharmaPermission;
import tiameds.pharmabackend.entity.PharmaRoles;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.UserFeaturePermission;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.mapper.UserDetailsMapper;
import tiameds.pharmabackend.repository.PharmaFeatureRepository;
import tiameds.pharmabackend.repository.PharmaPermissionRepository;
import tiameds.pharmabackend.repository.PharmaRolesRepository;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.UserFeaturePermissionRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.S3Service;
import tiameds.pharmabackend.service.UserDetailsService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final UserFeaturePermissionRepository userFeaturePermissionRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserDetailsMapper userDetailsMapper;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final CurrentPharmacyContext pharmacyContext;
    private final UserAuditRecorder userAuditRecorder;
    private final UserIdGeneratorService userIdGeneratorService;

    private static final DateTimeFormatter IMAGE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

//    @Override
//    public UserDetailsDto registerUser(UserDetailsDto userDetailsDto) {
//        Long roleId = 1L;
//
//        if (userDetailsDto.getPharmaRolesDto() != null
//                && userDetailsDto.getPharmaRolesDto().getRoleId() != null) {
//            roleId = userDetailsDto.getPharmaRolesDto().getRoleId();
//        }
//
//        PharmaRoles role = pharmaRolesRepository
//                .findById(roleId)
//                .orElseThrow(() -> new RuntimeException("Role not found"));
//
//        UserDetails user = userDetailsMapper.toEntity(userDetailsDto, role);
//
//        user.setPassword(passwordEncoder.encode(userDetailsDto.getPassword()));
//
//        user.setCreatedAt(LocalDateTime.now());
//        user.setIsRejected(Boolean.FALSE);
//        user.setUserStatus("Active");
//
//        UserDetails savedUser = userDetailsRepository.save(user);
//
//        return userDetailsMapper.toDto(savedUser);
//    }


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

        UserDetails user =
                userDetailsMapper.toEntity(userDetailsDto, role);

        // Generate user ID from backend
        user.setUserId(userIdGeneratorService.generateUserId());

        user.setPassword(
                passwordEncoder.encode(userDetailsDto.getPassword())
        );

        user.setCreatedAt(LocalDateTime.now());
        user.setIsRejected(Boolean.FALSE);
        user.setUserStatus("Active");

        UserDetails savedUser =
                userDetailsRepository.save(user);

        return userDetailsMapper.toDto(savedUser);
    }

//    @Override
//    public CreateUserResponseDto createUserWithPermissions(
//            Long currentUserId,
//            CreateUserRequestDto request) {
//
//        if (request.getUser() == null) {
//            throw new RuntimeException("User details are required");
//        }
//
//        UserDetails currentUser = userDetailsRepository
//                .findByUserIdWithOrganization(currentUserId)
//                .orElseThrow(() ->
//                        new RuntimeException("User not found with id : " + currentUserId));
//
//        if (currentUser.getOrganization() == null) {
//            throw new RuntimeException("User is not associated with any organization");
//        }
//
//        UserDetailsDto userDto = request.getUser();
//
//        if (userDto.getUserEmail() == null || userDto.getUserEmail().isBlank()) {
//            throw new RuntimeException("User email is required");
//        }
//
//        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
//            throw new RuntimeException("Password is required");
//        }
//
//        if (userDetailsRepository.existsByUserEmail(userDto.getUserEmail())) {
//            throw new RuntimeException(
//                    "User already exists with email : " + userDto.getUserEmail());
//        }
//
//        if (userDto.getPharmaRolesDto() == null
//                || userDto.getPharmaRolesDto().getRoleId() == null) {
//            throw new RuntimeException("Role is required");
//        }
//
//        Long roleId = userDto.getPharmaRolesDto().getRoleId();
//
//        PharmaRoles role = pharmaRolesRepository
//                .findById(roleId)
//                .orElseThrow(() ->
//                        new RuntimeException("Role not found with id : " + roleId));
//
//        UserDetails user = userDetailsMapper.toEntity(userDto, role);
//
//        user.setUserId(null);
//        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
//        user.setOrganization(currentUser.getOrganization());
//        user.setCreatedAt(LocalDateTime.now());
//        user.setIsRejected(Boolean.FALSE);
//        user.setUserStatus("Active");
//
//        attachPharmacies(
//                user,
//                request.getPharmacyIds(),
//                currentUser.getOrganization().getOrganizationId());
//
//        List<FeaturePermissionsDto> grantedPermissions =
//                attachPermissions(user, request.getPermissions());
//
//        UserDetails savedUser = userDetailsRepository.save(user);
//
//        return new CreateUserResponseDto(
//                userDetailsMapper.toDto(savedUser),
//                grantedPermissions);
//    }


    @Override
    public CreateUserResponseDto createUserWithPermissions(
            String currentUserId,
            CreateUserRequestDto request) {

        if (request.getUser() == null) {
            throw new RuntimeException("User details are required");
        }

        UserDetails currentUser = userDetailsRepository
                .findByUserIdWithOrganization(currentUserId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id : " + currentUserId
                        ));

        if (currentUser.getOrganization() == null) {
            throw new RuntimeException(
                    "User is not associated with any organization"
            );
        }

        UserDetailsDto userDto = request.getUser();

        if (userDto.getUserEmail() == null
                || userDto.getUserEmail().isBlank()) {

            throw new RuntimeException("User email is required");
        }

        if (userDto.getPassword() == null
                || userDto.getPassword().isBlank()) {

            throw new RuntimeException("Password is required");
        }

        if (userDetailsRepository.existsByUserEmail(
                userDto.getUserEmail())) {

            throw new RuntimeException(
                    "User already exists with email : "
                            + userDto.getUserEmail()
            );
        }

        if (userDto.getPharmaRolesDto() == null
                || userDto.getPharmaRolesDto().getRoleId() == null) {

            throw new RuntimeException("Role is required");
        }

        Long roleId = userDto.getPharmaRolesDto().getRoleId();

        PharmaRoles role = pharmaRolesRepository
                .findById(roleId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Role not found with id : " + roleId
                        ));

        UserDetails user =
                userDetailsMapper.toEntity(userDto, role);

        // Generate backend user ID
        user.setUserId(
                userIdGeneratorService.generateUserId()
        );

        user.setPassword(
                passwordEncoder.encode(userDto.getPassword())
        );

        user.setOrganization(
                currentUser.getOrganization()
        );

        user.setCreatedAt(LocalDateTime.now());
        user.setIsRejected(Boolean.FALSE);
        user.setUserStatus("Active");

        attachPharmacies(
                user,
                request.getPharmacyIds(),
                currentUser.getOrganization().getOrganizationId()
        );

        attachWarehouse(
                user,
                request.getWarehouseId(),
                currentUser.getOrganization().getOrganizationId()
        );

        List<FeaturePermissionsDto> grantedPermissions =
                attachPermissions(
                        user,
                        request.getPermissions()
                );

        UserDetails savedUser =
                userDetailsRepository.save(user);

        userAuditRecorder.record(
                UserAuditAction.USER_CREATED,
                currentUser,
                savedUser,
                "New user account created");

        return new CreateUserResponseDto(
                userDetailsMapper.toDto(savedUser),
                grantedPermissions
        );
    }

    private void attachPharmacies(
            UserDetails user,
            List<String> pharmacyIds,
            Long organizationId) {

        if (pharmacyIds == null || pharmacyIds.isEmpty()) {
            return;
        }

        Set<String> uniqueIds = new LinkedHashSet<>(pharmacyIds);
        uniqueIds.remove(null);

        if (uniqueIds.isEmpty()) {
            return;
        }

        Map<String, PharmacyDetails> pharmaciesById = pharmacyDetailsRepository
                .findAllById(uniqueIds)
                .stream()
                .collect(Collectors.toMap(
                        PharmacyDetails::getPharmacyId,
                        Function.identity()));

        for (String pharmacyId : uniqueIds) {

            PharmacyDetails pharmacy = pharmaciesById.get(pharmacyId);

            if (pharmacy == null) {
                throw new RuntimeException(
                        "Pharmacy not found with id : " + pharmacyId);
            }

            if (pharmacy.getOrganization() == null
                    || !organizationId.equals(
                            pharmacy.getOrganization().getOrganizationId())) {
                throw new RuntimeException(
                        "Pharmacy does not belong to your organization : " + pharmacyId);
            }

            user.getPharmacies().add(pharmacy);
        }
    }

    private void attachWarehouse(
            UserDetails user,
            String warehouseId,
            Long organizationId) {

        if (warehouseId == null || warehouseId.isBlank()) {
            return;
        }

        Warehouse warehouse = warehouseRepository
                .findById(warehouseId)
                .orElseThrow(() -> new RuntimeException(
                        "Warehouse not found with id : " + warehouseId));

        if (warehouse.getOrganization() == null
                || !organizationId.equals(
                        warehouse.getOrganization().getOrganizationId())) {
            throw new RuntimeException(
                    "Warehouse does not belong to your organization : " + warehouseId);
        }

        user.setWarehouse(warehouse);
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
    public List<UserSummaryDto> getAllUsers(String currentUserId) {

        Long organizationId = getOrganizationIdOfUser(currentUserId);

        return userDetailsRepository
                .findAllByOrganizationIdWithPharmacies(organizationId)
                .stream()
                .map(userDetailsMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailsDto getUserById(String currentUserId, String userId) {

        UserDetails user = getUserInSameOrganization(currentUserId, userId);

        return userDetailsMapper.toDto(user);
    }


    @Override
    public UserDetailsDto getById(String userId) {

        UserDetails user = userDetailsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userDetailsMapper.toDto(user);
    }

    @Override
    public List<FeaturePermissionsDto> updateUserPermissions(
            String currentUserId,
            String userId,
            AssignPermissionsRequestDto request) {

        UserDetails user = getUserInSameOrganization(currentUserId, userId);

        userFeaturePermissionRepository.deleteByUser_UserId(userId);
        userFeaturePermissionRepository.flush();

        user.getFeaturePermissions().clear();

        List<FeaturePermissionsDto> granted = attachPermissions(
                user,
                request == null ? null : request.getPermissions());

        userDetailsRepository.save(user);

        userAuditRecorder.record(
                UserAuditAction.PERMISSIONS_UPDATED,
                userDetailsRepository.findById(currentUserId).orElse(null),
                user,
                "Permissions updated (" + granted.size() + " features granted)");

        return granted;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturePermissionsDto> getUserPermissions(
            String currentUserId,
            String userId) {

        getUserInSameOrganization(currentUserId, userId);

        Map<Long, List<Long>> permissionsByFeature = new LinkedHashMap<>();

        for (UserFeaturePermission row :
                userFeaturePermissionRepository.findAllByUserIdWithFeature(userId)) {

            permissionsByFeature
                    .computeIfAbsent(
                            row.getFeature().getFeatureId(),
                            featureId -> new ArrayList<>())
                    .add(row.getPermission().getPermissionId());
        }

        List<FeaturePermissionsDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<Long>> entry : permissionsByFeature.entrySet()) {
            FeaturePermissionsDto dto = new FeaturePermissionsDto();
            dto.setFeatureId(entry.getKey());
            dto.setPermissionIds(entry.getValue());
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserPermissionsDto getCurrentUserPermissions(String currentUserId) {

        UserDetails user = userDetailsRepository
                .findById(currentUserId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id : " + currentUserId));

        List<String> permissionCodes = userFeaturePermissionRepository
                .findAllByUserIdWithFeature(currentUserId)
                .stream()
                .map(row -> row.getFeature().getFeatureCode()
                        + "_"
                        + row.getPermission().getPermissionName()
                                .trim()
                                .toUpperCase()
                                .replace(' ', '_'))
                .distinct()
                .collect(Collectors.toList());

        return new CurrentUserPermissionsDto(
                user.getUserId(),
                user.getRole().getRoleName(),
                permissionCodes);
    }

    @Override
    public UserImageDto uploadUserImage(
            String currentUserId,
            String userId,
            MultipartFile image) {

        return uploadUserImage(currentUserId, userId, image, false);
    }

    @Override
    public UserImageDto uploadUserImage(
            String currentUserId,
            String userId,
            MultipartFile image,
            boolean partOfCreate) {

        if (image == null || image.isEmpty()) {
            throw new RuntimeException("Image file is required");
        }

        String contentType = image.getContentType();

        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        UserDetails user = getUserInSameOrganization(currentUserId, userId);

        String key = buildUserImageKey(userId, image.getOriginalFilename());

        String imageUrl;

        try {
            imageUrl = s3Service.uploadFile(key, image);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }

        String oldImageUrl = user.getImageUrl();

        user.setImageUrl(imageUrl);
        userDetailsRepository.save(user);

        deleteOldImageQuietly(oldImageUrl);

        // An upload from the create-user wizard belongs to USER_CREATED, not to a
        // separate edit. The oldImageUrl check is the fallback for callers that
        // do not send the flag: a first image is part of setting the account up.
        boolean firstImage = oldImageUrl == null || oldImageUrl.isBlank();

        if (!partOfCreate && !firstImage) {

            userAuditRecorder.record(
                    UserAuditAction.USER_UPDATED,
                    userDetailsRepository.findById(currentUserId).orElse(null),
                    user,
                    "Profile image updated");
        }

        return new UserImageDto(user.getUserId(), imageUrl);
    }

    @Override
    public UserStatusDto updateUserStatus(
            String currentUserId,
            String userId,
            String userStatus) {

        String normalizedStatus = normalizeStatus(userStatus);

        if (currentUserId.equals(userId)
                && !"Active".equals(normalizedStatus)) {
            throw new RuntimeException("You cannot deactivate your own account");
        }

        UserDetails user = getUserInSameOrganization(currentUserId, userId);

        user.setUserStatus(normalizedStatus);
        user.setModifiedAt(LocalDateTime.now());
        user.setModifiedBy(currentUserId);

        userDetailsRepository.save(user);

        userAuditRecorder.record(
                UserAuditAction.USER_STATUS_CHANGED,
                userDetailsRepository.findById(currentUserId).orElse(null),
                user,
                "Status changed to " + normalizedStatus);

        return new UserStatusDto(user.getUserId(), user.getUserStatus());
    }

    private String normalizeStatus(String userStatus) {

        if (userStatus == null || userStatus.isBlank()) {
            throw new RuntimeException("User status is required");
        }

        String status = userStatus.trim();

        if ("Active".equalsIgnoreCase(status)) {
            return "Active";
        }

        if ("Inactive".equalsIgnoreCase(status)) {
            return "Inactive";
        }

        throw new RuntimeException(
                "Invalid user status : " + status + ". Allowed values are Active, Inactive");
    }

    private String buildUserImageKey(String userId, String originalFilename) {

        String extension = "";

        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex).toLowerCase();
            }
        }

        String timestamp = LocalDateTime.now().format(IMAGE_TIMESTAMP_FORMAT);

        return "users/" + userId + "/profile/PROFILE_" + timestamp + extension;
    }

    private void deleteOldImageQuietly(String oldImageUrl) {

        if (oldImageUrl == null || oldImageUrl.isBlank()) {
            return;
        }

        try {
            s3Service.deleteFile(s3Service.extractKeyFromUrl(oldImageUrl));
        } catch (Exception e) {
            // Old image may be external or already gone; replacing it should not fail the upload
        }
    }

    private UserDetails getUserInSameOrganization(String currentUserId, String userId) {

        Long organizationId = getOrganizationIdOfUser(currentUserId);

        return userDetailsRepository
                .findByUserIdWithOrganization(userId)
                .filter(u -> u.getOrganization() != null
                        && organizationId.equals(u.getOrganization().getOrganizationId()))
                .orElseThrow(() ->
                        new RuntimeException("User not found in your organization with id : " + userId));
    }

    private Long getOrganizationIdOfUser(String userId) {

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

    public boolean checkEmailExists(String email) {
        return userDetailsRepository.existsByUserEmail(email);
    }



    @Override
    public boolean checkEmployeeIdExists(String employeeId, UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return userDetailsRepository.existsByEmployeeIdAndPharmacyId(
                employeeId,
                pharmacyId
        );
    }
}