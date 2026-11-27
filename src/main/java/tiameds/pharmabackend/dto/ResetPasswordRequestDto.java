package tiameds.pharmabackend.dto;

import lombok.Data;

@Data
public class ResetPasswordRequestDto {

    private String userEmail;
    private String otp;
    private String newPassword;
}
