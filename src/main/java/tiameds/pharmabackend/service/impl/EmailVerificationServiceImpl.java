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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    // BUS-SR-011: OTP validity 5 minutes
    private static final int OTP_VALIDITY_MINUTES = 5;

    // BUS-SR-012: Maximum OTP attempts 5
    private static final int MAX_OTP_ATTEMPTS = 5;

    // BUS-SR-013: Maximum resend attempts 3
    private static final int MAX_RESEND_ATTEMPTS = 3;

    // BUS-SR-014: Lockout duration 30 minutes
    private static final int LOCK_DURATION_MINUTES = 30;

    private static final String TOO_MANY_ATTEMPTS_MESSAGE =
            "Too many verification attempts. Please try again after 30 minutes.";

    private static final String MAX_OTP_ATTEMPTS_MESSAGE =
            "You have entered an invalid OTP 5 times. "
                    + "Your verification is locked. Please try again after 30 minutes.";

    private final PharmaEmailVerificationOtpRepository otpRepository;
    private final OtpGenerator otpGenerator;
    private final MailService mailService;
    private final UserDetailsRepository userRepository;

    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public String sendOtp(EmailOtpRequestDto request) {

        if (userRepository.existsByUserEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        LocalDateTime now = LocalDateTime.now();

        Optional<PharmaEmailVerificationOtp> latestOpt =
                otpRepository.findTopByEmailOrderByIssuedAtDesc(
                        request.getEmail());

        int resendCount = 0;

        if (latestOpt.isPresent()) {

            PharmaEmailVerificationOtp latest = latestOpt.get();

            // Active Lock Check
            if (isCurrentlyLocked(latest, now)) {
                throw new RuntimeException(
                        TOO_MANY_ATTEMPTS_MESSAGE);
            }

            // Resend Within Same Verification Session
            if (!Boolean.TRUE.equals(latest.getIsLocked())
                    && !Boolean.TRUE.equals(latest.getIsUsed())
                    && latest.getIssuedAt().isAfter(
                    now.minusMinutes(LOCK_DURATION_MINUTES))) {

                resendCount = (latest.getResendCount() == null
                        ? 0
                        : latest.getResendCount()) + 1;

                if (resendCount > MAX_RESEND_ATTEMPTS) {

                    latest.setIsLocked(true);
                    latest.setLockedAt(now);

                    otpRepository.save(latest);

                    throw new RuntimeException(
                            TOO_MANY_ATTEMPTS_MESSAGE);
                }

                // Invalidate Previous OTP
                latest.setExpiredAt(now);

                otpRepository.save(latest);
            }
        }

        String otp = otpGenerator.generateOtp();

        PharmaEmailVerificationOtp entity =
                new PharmaEmailVerificationOtp();

        entity.setEmail(request.getEmail());
        entity.setOtp(otp);
        entity.setIssuedAt(now);
        entity.setExpiredAt(
                now.plusMinutes(OTP_VALIDITY_MINUTES));
        entity.setRetryCount(0);
        entity.setMaxRetryLimit(MAX_OTP_ATTEMPTS);
        entity.setResendCount(resendCount);
        entity.setIsLocked(false);
        entity.setIsUsed(false);
        entity.setIsVerified(false);

        otpRepository.save(entity);

        mailService.sendVerificationOtp(
                request.getEmail(),
                otp);

        return "OTP sent successfully";
    }

    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public String verifyOtp(EmailOtpVerifyRequestDto request) {

        LocalDateTime now = LocalDateTime.now();

        PharmaEmailVerificationOtp otpEntity =
                otpRepository
                        .findTopByEmailOrderByIssuedAtDesc(
                                request.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "OTP not found. Please request a new OTP"));

        // Active Lock Check (BUS-SR-014)
        if (isCurrentlyLocked(otpEntity, now)) {
            throw new RuntimeException(
                    TOO_MANY_ATTEMPTS_MESSAGE);
        }

        // OTP Already Used Check
        if (Boolean.TRUE.equals(otpEntity.getIsUsed())) {
            throw new RuntimeException(
                    "OTP already used. Please request a new OTP");
        }

        // OTP Expired Check (BUS-SR-010)
        if (now.isAfter(otpEntity.getExpiredAt())) {
            throw new RuntimeException(
                    "OTP expired. Please request a new OTP");
        }

        // Invalid OTP Check (BUS-SR-009, BUS-SR-012)
        if (!otpEntity.getOtp().equals(request.getOtp())) {

            otpEntity.setRetryCount(
                    otpEntity.getRetryCount() + 1);

            if (otpEntity.getRetryCount() >=
                    otpEntity.getMaxRetryLimit()) {

                otpEntity.setIsLocked(true);
                otpEntity.setLockedAt(now);

                otpRepository.save(otpEntity);

                throw new RuntimeException(
                        MAX_OTP_ATTEMPTS_MESSAGE);
            }

            otpRepository.save(otpEntity);

            int attemptsLeft = otpEntity.getMaxRetryLimit() - otpEntity.getRetryCount();

            throw new RuntimeException(
                    "Invalid OTP. Please try again. Attempts remaining: " + attemptsLeft);
        }

        // Mark Email As Verified (BUS-SR-007)
        otpEntity.setIsUsed(true);
        otpEntity.setUsedAt(now);
        otpEntity.setIsVerified(true);
        otpEntity.setVerifiedAt(now);

        otpRepository.save(otpEntity);

        return "Email Verified";
    }

    // Lock is active only within 30 minutes of lockedAt;
    // after that the user may request a new OTP.
    private boolean isCurrentlyLocked(
            PharmaEmailVerificationOtp otpEntity,
            LocalDateTime now) {

        return Boolean.TRUE.equals(otpEntity.getIsLocked())
                && otpEntity.getLockedAt() != null
                && now.isBefore(
                otpEntity.getLockedAt()
                        .plusMinutes(LOCK_DURATION_MINUTES));
    }
}
