package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class TherapeuticCategoryDto {
    private Long therapeuticCategoryId;
    private String therapeuticCategoryName;
    private Boolean isActive;
}
