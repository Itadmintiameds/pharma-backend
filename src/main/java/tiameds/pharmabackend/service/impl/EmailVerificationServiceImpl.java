package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.EmailOtpRequestDto;
import tiameds.pharmabackend.dto.EmailOtpVerifyRequestDto;
import tiameds.pharmabackend.entity.verifictaion.PharmaEmailVerificationOtp;
import tiameds.pharmabackend.repository.PharmaEmailVerificationOtpRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.EmailVerificationService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final PharmaEmailVerificationOtpRepository otpRepository;
    private final OtpGenerator otpGenerator;
    private final MailService mailService;
    private final UserDetailsRepository userRepository;

    @Override
    @Transactional
    public String sendOtp(EmailOtpRequestDto request) {

        if (userRepository.existsByUserEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        String otp =
                otpGenerator.generateOtp();

        PharmaEmailVerificationOtp entity =
                new PharmaEmailVerificationOtp();

        entity.setEmail(request.getEmail());
        entity.setOtp(otp);
        entity.setIssuedAt(LocalDateTime.now());
        entity.setExpiredAt(
                LocalDateTime.now().plusMinutes(5));
        entity.setRetryCount(0);
        entity.setMaxRetryLimit(3);
        entity.setIsLocked(false);
        entity.setIsUsed(false);

        otpRepository.save(entity);

        mailService.sendVerificationOtp(
                request.getEmail(),
                otp);

        return "OTP Sent";
    }

    @Override
    @Transactional
    public String verifyOtp(EmailOtpVerifyRequestDto request) {

        PharmaEmailVerificationOtp otpEntity =
                otpRepository
                        .findTopByEmailOrderByIssuedAtDesc(
                                request.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException("OTP not found"));

        // Account Locked Check
        if (Boolean.TRUE.equals(
                otpEntity.getIsLocked())) {

            throw new RuntimeException(
                    "Account Locked");
        }

        // OTP Expired Check
        if (LocalDateTime.now()
                .isAfter(
                        otpEntity.getExpiredAt())) {

            throw new RuntimeException(
                    "OTP Expired");
        }

        // OTP Already Used Check
        if (Boolean.TRUE.equals(
                otpEntity.getIsUsed())) {

            throw new RuntimeException(
                    "OTP Already Used");
        }

        // Invalid OTP Check
        if (!otpEntity.getOtp()
                .equals(request.getOtp())) {

            otpEntity.setRetryCount(
                    otpEntity.getRetryCount() + 1);

            if (otpEntity.getRetryCount() >=
                    otpEntity.getMaxRetryLimit()) {

                otpEntity.setIsLocked(true);
                otpEntity.setLockedAt(
                        LocalDateTime.now());
            }

            otpRepository.save(otpEntity);

            throw new RuntimeException(
                    "Invalid OTP");
        }

        // Mark OTP As Used
        otpEntity.setIsUsed(true);
        otpEntity.setUsedAt(
                LocalDateTime.now());

        otpRepository.save(otpEntity);

        return "Email Verified";
    }
}
