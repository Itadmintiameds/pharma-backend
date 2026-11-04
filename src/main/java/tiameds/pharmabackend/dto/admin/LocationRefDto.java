package tiameds.pharmabackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A pharmacy or warehouse reduced to what an operator needs to target it.
 */
@Data
@AllArgsConstructor
public class LocationRefDto {

    private String id;
    private String name;
}
