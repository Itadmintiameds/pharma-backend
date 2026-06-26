package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.LoginRequestDto;
import tiameds.pharmabackend.dto.LoginResponse;
import tiameds.pharmabackend.dto.OtpVerifyRequestDto;
import tiameds.pharmabackend.entity.PharmaOtp;
import tiameds.pharmabackend.entity.RefreshToken;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.repository.PharmaOtpRepository;
import tiameds.pharmabackend.repository.RefreshTokenRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserDetailsRepository userRepository;
    private final PharmaOtpRepository otpRepository;
    private final MailService mailService;
    private final OtpGenerator otpGenerator;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String login(LoginRequestDto request){

        UserDetails user =
                userRepository
                        .findByUserName(
                                request.getUserName())
                        .orElseThrow();

        if(!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())){

            throw new RuntimeException(
                    "Invalid Credentials");
        }

        String otp =
                otpGenerator.generateOtp();

        PharmaOtp entity =
                new PharmaOtp();

        entity.setUser(user);
        entity.setOtp(otp);
        entity.setIssuedAt(LocalDateTime.now());
        entity.setExpiredAt(
                LocalDateTime.now().plusMinutes(5));
        entity.setRetryCount(0);
        entity.setMaxRetryLimit(3);
        entity.setIsLocked(false);
        entity.setIsUsed(false);

        otpRepository.save(entity);

        mailService.sendOtp(
                user.getUserEmail(),
                otp);

        return "OTP Sent";
    }

    @Transactional
    public LoginResponse verifyOtp(
            OtpVerifyRequestDto request) {

        UserDetails user =
                userRepository
                        .findByUserName(
                                request.getUserName())
                        .orElseThrow(() ->
                                new RuntimeException("User not found"));

        PharmaOtp otpEntity =
                otpRepository
                        .findTopByUserOrderByIssuedAtDesc(user)
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

        // Revoke Existing Active Refresh Tokens
        List<RefreshToken> activeTokens =
                refreshTokenRepository
                        .findByUserAndIsRevokedFalse(user);

        for (RefreshToken token : activeTokens) {

            token.setIsRevoked(true);
        }

        refreshTokenRepository.saveAll(activeTokens);

        // Generate JWT Tokens
        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                jwtService.generateRefreshToken(user);

        // Save Refresh Token
        RefreshToken tokenEntity =
                new RefreshToken();

        tokenEntity.setUser(user);
        tokenEntity.setRefreshToken(refreshToken);
        tokenEntity.setIssuedAt(
                LocalDateTime.now());
        tokenEntity.setExpiredAt(
                LocalDateTime.now().plusDays(7));
        tokenEntity.setIsRevoked(false);
        tokenEntity.setCreatedAt(
                LocalDateTime.now());

        refreshTokenRepository.save(tokenEntity);

        return new LoginResponse(
                accessToken,
                refreshToken);
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken){

        RefreshToken tokenEntity =
                refreshTokenRepository
                        .findByRefreshToken(refreshToken)
                        .orElseThrow(() ->
                                new RuntimeException("Invalid Refresh Token"));

        if(Boolean.TRUE.equals(tokenEntity.getIsRevoked())){
            throw new RuntimeException("Token Revoked");
        }

        if(tokenEntity.getExpiredAt()
                .isBefore(LocalDateTime.now())){
            throw new RuntimeException("Token Expired");
        }

        UserDetails user =
                tokenEntity.getUser();

        String newAccessToken =
                jwtService.generateAccessToken(user);

        return new LoginResponse(
                newAccessToken,
                refreshToken);
    }

    @Transactional
    public void logout(String refreshToken){

        RefreshToken tokenEntity =
                refreshTokenRepository
                        .findByRefreshToken(refreshToken)
                        .orElseThrow();

        tokenEntity.setIsRevoked(true);

        refreshTokenRepository.save(tokenEntity);
    }
}
