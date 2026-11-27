package tiameds.pharmabackend.dto;

import lombok.Data;

@Data
public class ChangePasswordRequestDto {

    private String currentPassword;
    private String newPassword;
}
