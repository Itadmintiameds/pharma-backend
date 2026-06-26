package tiameds.pharmabackend.service.impl;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {

    public String generateOtp() {

        SecureRandom random = new SecureRandom();

        return String.valueOf(
                100000 + random.nextInt(900000));
    }
}
