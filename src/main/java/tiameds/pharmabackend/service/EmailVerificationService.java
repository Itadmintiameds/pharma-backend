package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.EmailOtpRequestDto;
import tiameds.pharmabackend.dto.EmailOtpVerifyRequestDto;

public interface EmailVerificationService {

    String sendOtp(EmailOtpRequestDto request);

    String verifyOtp(EmailOtpVerifyRequestDto request);
}
