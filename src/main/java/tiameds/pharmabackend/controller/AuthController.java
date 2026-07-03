package tiameds.pharmabackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.pharmabackend.dto.LoginRequestDto;
import tiameds.pharmabackend.dto.LoginResponse;
import tiameds.pharmabackend.dto.OtpVerifyRequestDto;
import tiameds.pharmabackend.service.impl.AuthService;
import jakarta.servlet.http.Cookie;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequestDto request){

        return ResponseEntity.ok(
                authService.login(request));
    }

    private ResponseCookie buildCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .domain("tiameds.ai")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    @PostMapping("/verifyOtp")
    public ResponseEntity<String> verifyOtp(
            @RequestBody OtpVerifyRequestDto request,
            HttpServletResponse response) {

        LoginResponse loginResponse =
                authService.verifyOtp(request);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildCookie(
                        "access_token",
                        loginResponse.getAccessToken(),
                        30 * 60
                ).toString());

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildCookie(
                        "refresh_token",
                        loginResponse.getRefreshToken(),
                        7 * 24 * 60 * 60
                ).toString());

        return ResponseEntity.ok("Login successful");
    }


    @PostMapping("/refreshToken")
    public ResponseEntity<String> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = null;

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            throw new RuntimeException("Refresh Token Missing");
        }

        LoginResponse loginResponse =
                authService.refreshToken(refreshToken);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildCookie(
                        "access_token",
                        loginResponse.getAccessToken(),
                        30 * 60
                ).toString());

        return ResponseEntity.ok("Access Token Refreshed");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = null;

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildCookie(
                        "access_token",
                        "",
                        0
                ).toString());

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildCookie(
                        "refresh_token",
                        "",
                        0
                ).toString());

        return ResponseEntity.ok("Logged Out");
    }
}