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
            Long currentUserId,
            CreateUserRequestDto request);

    List<UserSummaryDto> getAllUsers(Long currentUserId);

    UserDetailsDto getUserById(Long currentUserId, Long userId);

    UserDetailsDto getById(Long userId);

    List<FeaturePermissionsDto> updateUserPermissions(
            Long currentUserId,
            Long userId,
            AssignPermissionsRequestDto request);

    List<FeaturePermissionsDto> getUserPermissions(
            Long currentUserId,
            Long userId);

    CurrentUserPermissionsDto getCurrentUserPermissions(Long currentUserId);

    UserImageDto uploadUserImage(
            Long currentUserId,
            Long userId,
            MultipartFile image);

    UserStatusDto updateUserStatus(
            Long currentUserId,
            Long userId,
            String userStatus);

    boolean checkEmailExists(String email);

    boolean checkEmployeeIdExists(String employeeId, UserDetails user);

//    void deleteUserByPharmacyRegistrationId(String pharmacyRegistrationId);


}
