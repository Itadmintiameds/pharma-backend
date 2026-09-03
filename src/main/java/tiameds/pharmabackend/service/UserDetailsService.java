package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.AssignPermissionsRequestDto;
import tiameds.pharmabackend.dto.CreateUserRequestDto;
import tiameds.pharmabackend.dto.CreateUserResponseDto;
import tiameds.pharmabackend.dto.CurrentUserPermissionsDto;
import tiameds.pharmabackend.dto.FeaturePermissionsDto;
import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.dto.UpdateUserRequestDto;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.dto.UserImageDto;
import tiameds.pharmabackend.dto.UserStatusDto;
import tiameds.pharmabackend.dto.UserSummaryDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface UserDetailsService {

    /**
     * @param ipAddress best-effort client address, recorded on the consent row.
     *                  Resolved in the controller, where the request is available.
     */
    UserDetailsDto registerUser(UserDetailsDto userDetailsDto, String ipAddress);

    CreateUserResponseDto createUserWithPermissions(
            String currentUserId,
            CreateUserRequestDto request);

    List<UserSummaryDto> getAllUsers(String currentUserId);

    UserDetailsDto getUserById(String currentUserId, String userId);

    /** Partial edit of an existing user. Email and password are not editable. */
    UserDetailsDto updateUser(
            String currentUserId,
            String userId,
            UpdateUserRequestDto request);

    UserDetailsDto getById(String userId);

    List<FeaturePermissionsDto> updateUserPermissions(
            String currentUserId,
            String userId,
            AssignPermissionsRequestDto request);

    List<FeaturePermissionsDto> getUserPermissions(
            String currentUserId,
            String userId);

    CurrentUserPermissionsDto getCurrentUserPermissions(String currentUserId);

    UserImageDto uploadUserImage(
            String currentUserId,
            String userId,
            MultipartFile image);

    /**
     * partOfCreate marks an upload made by the create-user wizard, so the image
     * is treated as part of the account creation instead of a later edit.
     */
    UserImageDto uploadUserImage(
            String currentUserId,
            String userId,
            MultipartFile image,
            boolean partOfCreate);

    UserStatusDto updateUserStatus(
            String currentUserId,
            String userId,
            String userStatus);

    boolean checkEmailExists(String email);

    boolean checkEmployeeIdExists(String employeeId, UserDetails user);

//    void deleteUserByPharmacyRegistrationId(String pharmacyRegistrationId);


}
