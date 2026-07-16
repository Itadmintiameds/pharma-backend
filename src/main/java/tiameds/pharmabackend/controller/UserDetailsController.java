package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.UserDetailsDto;
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

    @GetMapping("/all")
    public ResponseEntity<List<UserSummaryDto>> getAllUsers(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(
                userDetailsService.getAllUsers(currentUser.getUserId()));
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