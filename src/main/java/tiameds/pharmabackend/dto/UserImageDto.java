package tiameds.pharmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserImageDto {

    private String userId;
    private String imageUrl;
}
