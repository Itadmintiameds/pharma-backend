package tiameds.pharmabackend.dto;

import lombok.Data;

@Data
public class EmailOtpVerifyRequestDto {

    private String email;
    private String otp;
}
