package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.audit.UserAuditRecorder;
import tiameds.pharmabackend.dto.ChangePasswordRequestDto;
import tiameds.pharmabackend.dto.ForgotPasswordRequestDto;
import tiameds.pharmabackend.dto.ResetPasswordRequestDto;
import tiameds.pharmabackend.entity.RefreshToken;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.verifictaion.PharmaPasswordResetOtp;
import tiameds.pharmabackend.enums.UserAuditAction;
import tiameds.pharmabackend.repository.PharmaPasswordResetOtpRepository;
import tiameds.pharmabackend.repository.RefreshTokenRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.service.PasswordService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    // Same limits as email verification (BUS-SR-011..014).
    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int MAX_RESEND_ATTEMPTS = 3;
    private static final int LOCK_DURATION_MINUTES = 30;

    private static final int MIN_PASSWORD_LENGTH = 8;
    // BCrypt ignores input past 72 bytes.
    private static final int MAX_PASSWORD_LENGTH = 64;

    // Identical for unknown, inactive, locked and valid accounts, so the
    // endpoint cannot be used to find out which emails are registered.
    private static final String FORGOT_PASSWORD_RESPONSE =
            "If an account exists for this email, a password reset OTP has been sent";

    private static final String INVALID_OTP_MESSAGE =
            "Invalid or expired OTP. Please request a new OTP";

    private static final String TOO_MANY_ATTEMPTS_MESSAGE =
            "Too many attempts. Please try again after 30 minutes.";

    private static final String MAX_OTP_ATTEMPTS_MESSAGE =
            "You have entered an invalid OTP 5 times. "
                    + "Password reset is locked. Please try again after 30 minutes.";

    private final UserDetailsRepository userRepository;
    private final PharmaPasswordResetOtpRepository otpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpGenerator otpGenerator;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final UserAuditRecorder userAuditRecorder;

    @Override
    @Transactional
    public String forgotPassword(ForgotPasswordRequestDto request) {

        String email = normalizeEmail(request == null ? null : request.getUserEmail());

        Optional<UserDetails> userOpt = userRepository.findByUserEmail(email);

        if (userOpt.isEmpty() || !isActive(userOpt.get())) {
            return FORGOT_PASSWORD_RESPONSE;
        }

        UserDetails user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();
        int resendCount = 0;

        Optional<PharmaPasswordResetOtp> latestOpt =
                otpRepository.findTopByUserOrderByIssuedAtDesc(user);

        if (latestOpt.isPresent()) {

            PharmaPasswordResetOtp latest = latestOpt.get();

            // Locked: silently send nothing (same response, see above).
            if (isCurrentlyLocked(latest, now)) {
                return FORGOT_PASSWORD_RESPONSE;
            }

            // Resend within the same reset session
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

                    return FORGOT_PASSWORD_RESPONSE;
                }

                // Invalidate the previous OTP
                latest.setExpiredAt(now);
                otpRepository.save(latest);
            }
        }

        String otp = otpGenerator.generateOtp();

        PharmaPasswordResetOtp entity = new PharmaPasswordResetOtp();
        entity.setUser(user);
        entity.setOtp(otp);
        entity.setIssuedAt(now);
        entity.setExpiredAt(now.plusMinutes(OTP_VALIDITY_MINUTES));
        entity.setRetryCount(0);
        entity.setMaxRetryLimit(MAX_OTP_ATTEMPTS);
        entity.setResendCount(resendCount);
        entity.setIsLocked(false);
        entity.setIsUsed(false);

        otpRepository.save(entity);

        mailService.sendPasswordResetOtp(user.getUserEmail(), otp);

        return FORGOT_PASSWORD_RESPONSE;
    }

    @Override
    // Keep retry-count / lock updates when an attempt fails.
    @Transactional(noRollbackFor = RuntimeException.class)
    public String resetPassword(ResetPasswordRequestDto request) {

        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        String email = normalizeEmail(request.getUserEmail());

        if (request.getOtp() == null || request.getOtp().isBlank()) {
            throw new IllegalArgumentException("OTP is required");
        }

        // Validate before touching the OTP so a weak password doesn't burn an attempt.
        validatePasswordPolicy(request.getNewPassword());

        UserDetails user = userRepository.findByUserEmail(email)
                .filter(this::isActive)
                .orElseThrow(() -> new RuntimeException(INVALID_OTP_MESSAGE));

        PharmaPasswordResetOtp otpEntity =
                otpRepository.findTopByUserOrderByIssuedAtDesc(user)
                        .orElseThrow(() -> new RuntimeException(INVALID_OTP_MESSAGE));

        LocalDateTime now = LocalDateTime.now();

        if (isCurrentlyLocked(otpEntity, now)) {
            throw new RuntimeException(TOO_MANY_ATTEMPTS_MESSAGE);
        }

        if (Boolean.TRUE.equals(otpEntity.getIsUsed())
                || now.isAfter(otpEntity.getExpiredAt())) {
            throw new RuntimeException(INVALID_OTP_MESSAGE);
        }

        if (!otpEntity.getOtp().equals(request.getOtp().trim())) {

            otpEntity.setRetryCount(otpEntity.getRetryCount() + 1);

            if (otpEntity.getRetryCount() >= otpEntity.getMaxRetryLimit()) {

                otpEntity.setIsLocked(true);
                otpEntity.setLockedAt(now);
                otpRepository.save(otpEntity);

                throw new RuntimeException(MAX_OTP_ATTEMPTS_MESSAGE);
            }

            otpRepository.save(otpEntity);

            int attemptsLeft = otpEntity.getMaxRetryLimit() - otpEntity.getRetryCount();

            throw new RuntimeException(
                    "Invalid OTP. Please try again. Attempts remaining: " + attemptsLeft);
        }

        // OTP is valid but not consumed yet, so the user can retry with another password.
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException(
                    "New password must be different from the current password");
        }

        otpEntity.setIsUsed(true);
        otpEntity.setUsedAt(now);
        otpRepository.save(otpEntity);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        revokeRefreshTokens(user, null);

        userAuditRecorder.recordSelf(
                UserAuditAction.PASSWORD_RESET,
                user,
                "Password reset via email OTP");

        return "Password reset successfully. Please sign in with your new password";
    }

    @Override
    @Transactional
    public String changePassword(
            String userId,
            ChangePasswordRequestDto request,
            String currentRefreshToken) {

        if (request == null
                || request.getCurrentPassword() == null
                || request.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("Current password is required");
        }

        validatePasswordPolicy(request.getNewPassword());

        UserDetails user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException(
                    "New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        revokeRefreshTokens(user, currentRefreshToken);

        userAuditRecorder.recordSelf(
                UserAuditAction.PASSWORD_SET,
                user,
                "Password changed");

        return "Password changed successfully";
    }

    /**
     * Revokes the user's active refresh tokens, except {@code keepToken} when
     * given. Access tokens are stateless and lapse on their own (30 min).
     */
    private void revokeRefreshTokens(UserDetails user, String keepToken) {

        List<RefreshToken> activeTokens =
                refreshTokenRepository.findByUserAndIsRevokedFalse(user);

        for (RefreshToken token : activeTokens) {
            if (keepToken == null || !keepToken.equals(token.getRefreshToken())) {
                token.setIsRevoked(true);
            }
        }

        refreshTokenRepository.saveAll(activeTokens);
    }

    private void validatePasswordPolicy(String password) {

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }

        if (password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be between " + MIN_PASSWORD_LENGTH
                            + " and " + MAX_PASSWORD_LENGTH + " characters");
        }
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        return email.trim();
    }

    private boolean isActive(UserDetails user) {
        return "Active".equalsIgnoreCase(user.getUserStatus());
    }

    // Lock is active only within 30 minutes of lockedAt.
    private boolean isCurrentlyLocked(PharmaPasswordResetOtp otpEntity, LocalDateTime now) {

        return Boolean.TRUE.equals(otpEntity.getIsLocked())
                && otpEntity.getLockedAt() != null
                && now.isBefore(otpEntity.getLockedAt().plusMinutes(LOCK_DURATION_MINUTES));
    }
}
