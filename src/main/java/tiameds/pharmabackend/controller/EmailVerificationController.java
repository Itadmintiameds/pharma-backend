package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.EmailOtpRequestDto;
import tiameds.pharmabackend.dto.EmailOtpVerifyRequestDto;
import tiameds.pharmabackend.service.EmailVerificationService;

@RestController
@RequestMapping("/verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/sendOtp")
    public ResponseEntity<String> sendOtp(
            @RequestBody EmailOtpRequestDto request) {

        return ResponseEntity.ok(
                emailVerificationService.sendOtp(request));
    }

    @PostMapping("/verifyOtp")
    public ResponseEntity<String> verifyOtp(
            @RequestBody EmailOtpVerifyRequestDto request) {

        return ResponseEntity.ok(
                emailVerificationService.verifyOtp(request));
    }
}
