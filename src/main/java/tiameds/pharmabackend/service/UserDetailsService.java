package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.AssignPermissionsRequestDto;
import tiameds.pharmabackend.dto.CreateUserRequestDto;
import tiameds.pharmabackend.dto.CreateUserResponseDto;
import tiameds.pharmabackend.dto.CurrentUserPermissionsDto;
import tiameds.pharmabackend.dto.FeaturePermissionsDto;
import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.dto.UserImageDto;
import tiameds.pharmabackend.dto.UserStatusDto;
import tiameds.pharmabackend.dto.UserSummaryDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface UserDetailsService {

    UserDetailsDto registerUser(UserDetailsDto userDetailsDto);

    CreateUserResponseDto createUserWithPermissions(
            String currentUserId,
            CreateUserRequestDto request);

    List<UserSummaryDto> getAllUsers(String currentUserId);

    UserDetailsDto getUserById(String currentUserId, String userId);

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

    UserStatusDto updateUserStatus(
            String currentUserId,
            String userId,
            String userStatus);

    boolean checkEmailExists(String email);

    boolean checkEmployeeIdExists(String employeeId, UserDetails user);

//    void deleteUserByPharmacyRegistrationId(String pharmacyRegistrationId);


}
