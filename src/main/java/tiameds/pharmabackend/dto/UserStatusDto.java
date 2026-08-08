package tiameds.pharmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusDto {

    private String userId;
    private String userStatus;  // Active or Inactive
}
