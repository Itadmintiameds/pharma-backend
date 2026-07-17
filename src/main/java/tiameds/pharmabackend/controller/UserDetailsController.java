package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.AssignPermissionsRequestDto;
import tiameds.pharmabackend.dto.CreateUserRequestDto;
import tiameds.pharmabackend.dto.CreateUserResponseDto;
import tiameds.pharmabackend.dto.CurrentUserPermissionsDto;
import tiameds.pharmabackend.dto.FeaturePermissionsDto;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.dto.UserDetailsDto;
import tiameds.pharmabackend.dto.UserImageDto;
import tiameds.pharmabackend.dto.UserStatusDto;
import tiameds.pharmabackend.dto.UserSummaryDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.UserDetailsService;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDetailsController {

    private final UserDetailsService userDetailsService;

    @PostMapping("/registration")
    public ResponseEntity<UserDetailsDto> registerUser(
            @RequestBody UserDetailsDto userDetailsDto) {
        System.out.println("CONTROLLER HIT");
        UserDetailsDto response =
                userDetailsService.registerUser(userDetailsDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/create")
    public ResponseEntity<CreateUserResponseDto> createUser(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody CreateUserRequestDto request) {

        CreateUserResponseDto response =
                userDetailsService.createUserWithPermissions(
                        currentUser.getUserId(),
                        request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserSummaryDto>> getAllUsers(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(
                userDetailsService.getAllUsers(currentUser.getUserId()));
    }

    @GetMapping("/me/permissions")
    public ResponseEntity<CurrentUserPermissionsDto> getMyPermissions(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(
                userDetailsService.getCurrentUserPermissions(currentUser.getUserId()));
    }

    @GetMapping("/{userId}/permissions")
    public ResponseEntity<List<FeaturePermissionsDto>> getUserPermissions(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                userDetailsService.getUserPermissions(currentUser.getUserId(), userId));
    }

    @PutMapping("/{userId}/permissions")
    public ResponseEntity<List<FeaturePermissionsDto>> updateUserPermissions(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId,
            @RequestBody AssignPermissionsRequestDto request) {

        return ResponseEntity.ok(
                userDetailsService.updateUserPermissions(
                        currentUser.getUserId(),
                        userId,
                        request));
    }

    @PostMapping(
            value = "/{userId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserImageDto> uploadUserImage(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId,
            @RequestParam("image") MultipartFile image) {

        return ResponseEntity.ok(
                userDetailsService.uploadUserImage(
                        currentUser.getUserId(),
                        userId,
                        image));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserStatusDto> updateUserStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId,
            @RequestBody UserStatusDto request) {

        return ResponseEntity.ok(
                userDetailsService.updateUserStatus(
                        currentUser.getUserId(),
                        userId,
                        request.getUserStatus()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailsDto> getUserById(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                userDetailsService.getUserById(currentUser.getUserId(), userId));
    }

//    @DeleteMapping("/delete/{pharmacyRegistrationId}")
//    public ResponseEntity<String> deleteUser(
//            @PathVariable String pharmacyRegistrationId) {
//
//        userDetailsService.deleteUserByPharmacyRegistrationId(pharmacyRegistrationId);
//
//        return ResponseEntity.ok("User deleted successfully.");
//    }
}