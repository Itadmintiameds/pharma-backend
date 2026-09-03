package tiameds.pharmabackend.service;

import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.dto.legal.TermsPolicyAdminDto;
import tiameds.pharmabackend.dto.legal.TermsPolicyDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.time.LocalDateTime;
import java.util.List;

public interface TermsPolicyService {

    /** The live version. Throws if nothing has been published yet. */
    TermsPolicyDto getCurrent();

    TermsPolicyDto getByVersion(String version);

    List<TermsPolicyAdminDto> listAll();

    /** Uploads the document to S3 and records a DRAFT version. */
    TermsPolicyAdminDto upload(
            MultipartFile file,
            String version,
            String title,
            LocalDateTime effectiveFrom,
            boolean requiresReacceptance,
            String createdBy);

    /**
     * Archives the current ACTIVE version and makes this one live.
     * The sole enforcement point for "at most one ACTIVE version".
     */
    TermsPolicyAdminDto publish(Long id);

    /**
     * Records the consent submitted at registration against the live version.
     * <p>
     * Non-blocking by design: the flag is stored as sent (true or false) and a
     * missing ACTIVE version is skipped rather than failing the signup, so
     * registration never breaks on the policy state.
     */
    void recordRegistrationAcceptance(
            UserDetails user,
            Boolean acceptedTerms,
            String ipAddress);

    /**
     * Stamps the organization onto acceptance rows recorded before it existed.
     * Called once the organization is created.
     */
    void backfillOrganization(String userId, Long organizationId);
}
