package tiameds.pharmabackend.dto;

import lombok.Data;

@Data
public class OtpVerifyRequestDto {

    private String userEmail;
    private String otp;
}
