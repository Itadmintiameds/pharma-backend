package tiameds.pharmabackend.dto;

import lombok.Data;

@Data
public class LoginRequestDto {

    private String userEmail;
    private String password;
}
