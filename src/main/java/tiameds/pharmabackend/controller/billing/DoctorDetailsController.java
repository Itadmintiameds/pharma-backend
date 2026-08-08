package tiameds.pharmabackend.controller.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.billing.DoctorDetailsDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.billing.DoctorDetailsService;

import java.util.List;

@RestController
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorDetailsController {

    private final DoctorDetailsService doctorDetailsService;

    @PostMapping("/create")
    public ResponseEntity<?> createDoctor(
            @RequestBody DoctorDetailsDto doctorDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        DoctorDetailsDto response = doctorDetailsService.createDoctor(
                doctorDto,
                currentUser.getUser());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/allDoctor")
    public ResponseEntity<?> getAllDoctors(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<DoctorDetailsDto> doctors =
                doctorDetailsService.getAllDoctors(currentUser.getUser());

        return ResponseEntity.ok(doctors);
    }


    @GetMapping("/getById/{doctorId}")
    public ResponseEntity<?> getDoctorById(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        DoctorDetailsDto doctor = doctorDetailsService.getDoctorById(
                doctorId,
                currentUser.getUser());

        return ResponseEntity.ok(doctor);
    }
}
