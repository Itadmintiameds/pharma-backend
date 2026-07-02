package tiameds.pharmabackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PharmaDocumentsDto {

    private Long documentId;
    private String documentNo;
    private String documentType;
    private String documentUrl;
    private LocalDateTime issueDate;
    private String issueAuthority;
    private LocalDateTime expiryDate;
    private Boolean isActive;

}
