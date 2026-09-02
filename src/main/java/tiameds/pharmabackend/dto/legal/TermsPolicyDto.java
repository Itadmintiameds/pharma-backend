package tiameds.pharmabackend.dto.legal;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Public view of a policy version, returned by /terms/current and
 * /terms/versions/{version}. Deliberately omits status and audit fields.
 */
@Data
public class TermsPolicyDto {

    private Long id;
    private String version;
    private String title;
    private LocalDateTime effectiveFrom;
    private String documentUrl;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String contentHash;
}
