package tiameds.pharmabackend.service;

import tiameds.pharmabackend.dto.ChangePasswordRequestDto;
import tiameds.pharmabackend.dto.ForgotPasswordRequestDto;
import tiameds.pharmabackend.dto.ResetPasswordRequestDto;

public interface PasswordService {

    /** Sends a reset OTP if the account exists and is active. Always returns the same message. */
    String forgotPassword(ForgotPasswordRequestDto request);

    /** Verifies the reset OTP, sets the new password and signs the user out everywhere. */
    String resetPassword(ResetPasswordRequestDto request);

    /**
     * Changes the signed-in user's password. Other sessions are signed out;
     * the session holding {@code currentRefreshToken} (may be null) is kept.
     */
    String changePassword(String userId, ChangePasswordRequestDto request, String currentRefreshToken);
}
