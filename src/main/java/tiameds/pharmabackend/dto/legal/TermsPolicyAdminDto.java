package tiameds.pharmabackend.dto.legal;

import lombok.Data;
import tiameds.pharmabackend.enums.PolicyStatus;

import java.time.LocalDateTime;

/** Admin view: everything in the public DTO plus lifecycle and audit fields. */
@Data
public class TermsPolicyAdminDto {

    private Long id;
    private String version;
    private String title;
    private LocalDateTime effectiveFrom;
    private String documentUrl;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String contentHash;
    private PolicyStatus status;
    private Boolean requiresReacceptance;
    private LocalDateTime createdAt;
    private String createdBy;
}
