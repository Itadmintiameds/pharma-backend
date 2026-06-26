package tiameds.pharmabackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/verifyOtp")
    public ResponseEntity<String> verifyOtp(
            @RequestBody OtpVerifyRequestDto request,
            HttpServletResponse response) {

        LoginResponse loginResponse =
                authService.verifyOtp(request);

        Cookie accessTokenCookie =
                new Cookie(
                        "access_token",
                        loginResponse.getAccessToken());

        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true); // HTTPS only
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(30 * 60); // 30 mins

        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie =
                new Cookie(
                        "refresh_token",
                        loginResponse.getRefreshToken());

        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days

        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(
                "Login successful");
    }


    @PostMapping("/refreshToken")
    public ResponseEntity<String> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = null;

        Cookie[] cookies =
                request.getCookies();

        if(cookies != null){

            for(Cookie cookie : cookies){

                if("refresh_token".equals(
                        cookie.getName())){

                    refreshToken =
                            cookie.getValue();
                }
            }
        }

        if(refreshToken == null){
            throw new RuntimeException(
                    "Refresh Token Missing");
        }

        LoginResponse loginResponse =
                authService.refreshToken(
                        refreshToken);

        Cookie accessCookie =
                new Cookie(
                        "access_token",
                        loginResponse.getAccessToken());

        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(1800);

        response.addCookie(accessCookie);

        return ResponseEntity.ok(
                "Access Token Refreshed");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpServletRequest request,
            HttpServletResponse response){

        String refreshToken = null;

        Cookie[] cookies =
                request.getCookies();

        if(cookies != null){

            for(Cookie cookie : cookies){

                if("refresh_token".equals(
                        cookie.getName())){

                    refreshToken =
                            cookie.getValue();
                }
            }
        }

        authService.logout(refreshToken);

        Cookie access =
                new Cookie(
                        "access_token", null);

        access.setMaxAge(0);
        access.setPath("/");

        Cookie refresh =
                new Cookie(
                        "refresh_token", null);

        refresh.setMaxAge(0);
        refresh.setPath("/");

        response.addCookie(access);
        response.addCookie(refresh);

        return ResponseEntity.ok("Logged Out");
    }
}