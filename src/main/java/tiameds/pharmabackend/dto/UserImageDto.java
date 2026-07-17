package tiameds.pharmabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserImageDto {

    private Long userId;
    private String imageUrl;
}
