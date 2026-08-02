package tiameds.pharmabackend.dto.master;

import lombok.Data;

@Data
public class TherapeuticSubcategoryDto {
    private Long therapeuticSubcategoryId;
    private String therapeuticSubcategoryName;
    private Long therapeuticCategoryId;
    private String therapeuticCategoryName;
    private Boolean isActive;
}
