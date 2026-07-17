package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.AssignPermissionsRequestDto;
import tiameds.pharmabackend.dto.CreateUserRequestDto;
import tiameds.pharmabackend.dto.CreateUserResponseDto;
import tiameds.pharmabackend.dto.CurrentUserPermissionsDto;
import tiameds.pharmabackend.dto.FeaturePermissionsDto;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.dto.UserSummaryDto;

import java.util.List;

public interface UserDetailsService {

    UserDetailsDto registerUser(UserDetailsDto userDetailsDto);

    CreateUserResponseDto createUserWithPermissions(
            Long currentUserId,
            CreateUserRequestDto request);

    List<UserSummaryDto> getAllUsers(Long currentUserId);

    UserDetailsDto getUserById(Long currentUserId, Long userId);

    List<FeaturePermissionsDto> updateUserPermissions(
            Long currentUserId,
            Long userId,
            AssignPermissionsRequestDto request);

    List<FeaturePermissionsDto> getUserPermissions(
            Long currentUserId,
            Long userId);

    CurrentUserPermissionsDto getCurrentUserPermissions(Long currentUserId);

//    void deleteUserByPharmacyRegistrationId(String pharmacyRegistrationId);


}
