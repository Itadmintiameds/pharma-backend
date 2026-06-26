package tiameds.pharmabackend.dto;

import lombok.Data;

@Data
public class OtpVerifyRequestDto {

    private String userName;
    private String otp;
}
