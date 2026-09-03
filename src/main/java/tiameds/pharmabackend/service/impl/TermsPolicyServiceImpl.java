package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.dto.legal.TermsPolicyAdminDto;
import tiameds.pharmabackend.dto.legal.TermsPolicyDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.legal.PharmaTermsPolicy;
import tiameds.pharmabackend.entity.legal.PharmaUserPolicyAcceptance;
import tiameds.pharmabackend.enums.PolicyStatus;
import tiameds.pharmabackend.exception.ResourceNotFoundException;
import tiameds.pharmabackend.repository.PharmaTermsPolicyRepository;
import tiameds.pharmabackend.repository.PharmaUserPolicyAcceptanceRepository;
import tiameds.pharmabackend.service.S3Service;
import tiameds.pharmabackend.service.TermsPolicyService;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class TermsPolicyServiceImpl implements TermsPolicyService {

    private final PharmaTermsPolicyRepository policyRepository;
    private final PharmaUserPolicyAcceptanceRepository acceptanceRepository;
    private final S3Service s3Service;

    @Override
    @Transactional(readOnly = true)
    public TermsPolicyDto getCurrent() {
        return toDto(requireActivePolicy());
    }

    @Override
    @Transactional(readOnly = true)
    public TermsPolicyDto getByVersion(String version) {
        PharmaTermsPolicy policy = policyRepository.findByVersion(version)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No terms & privacy policy found with version: " + version));

        return toDto(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermsPolicyAdminDto> listAll() {
        return policyRepository.findAllByOrderByEffectiveFromDesc()
                .stream()
                .map(this::toAdminDto)
                .toList();
    }

    @Override
    public TermsPolicyAdminDto upload(
            MultipartFile file,
            String version,
            String title,
            LocalDateTime effectiveFrom,
            boolean requiresReacceptance,
            String createdBy) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file is required");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version is required");
        }
        if (policyRepository.existsByVersion(version)) {
            throw new IllegalArgumentException("Version already exists: " + version);
        }
        if (effectiveFrom == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded document", e);
        }

        String documentUrl;
        try {
            documentUrl = s3Service.uploadFile(buildDocumentKey(version, file), file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload document to S3", e);
        }

        PharmaTermsPolicy policy = new PharmaTermsPolicy();
        policy.setVersion(version);
        policy.setTitle(title);
        policy.setDocumentUrl(documentUrl);
        policy.setFileName(file.getOriginalFilename());
        policy.setContentType(file.getContentType());
        policy.setFileSize(file.getSize());
        policy.setContentHash(sha256Hex(bytes));
        policy.setEffectiveFrom(effectiveFrom);
        policy.setStatus(PolicyStatus.DRAFT);
        policy.setRequiresReacceptance(requiresReacceptance);
        policy.setCreatedAt(LocalDateTime.now());
        policy.setCreatedBy(createdBy);

        return toAdminDto(policyRepository.save(policy));
    }

    @Override
    public TermsPolicyAdminDto publish(Long id) {

        PharmaTermsPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No terms & privacy policy found with id: " + id));

        if (policy.getStatus() == PolicyStatus.ACTIVE) {
            throw new IllegalStateException("Version " + policy.getVersion() + " is already active");
        }
        if (policy.getStatus() == PolicyStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Version " + policy.getVersion() + " is archived and cannot be re-published; "
                            + "upload a new version instead");
        }

        // Archive the outgoing version first, so the two rows are never both ACTIVE.
        policyRepository.findFirstByStatus(PolicyStatus.ACTIVE)
                .ifPresent(current -> {
                    current.setStatus(PolicyStatus.ARCHIVED);
                    policyRepository.save(current);
                });

        policy.setStatus(PolicyStatus.ACTIVE);

        return toAdminDto(policyRepository.save(policy));
    }

    @Override
    public void recordRegistrationAcceptance(
            UserDetails user,
            Boolean acceptedTerms,
            String ipAddress) {

        // Nothing published yet means there is nothing to record against. Let the
        // signup through rather than blocking it on the policy state.
        Optional<PharmaTermsPolicy> current =
                policyRepository.findFirstByStatus(PolicyStatus.ACTIVE);

        if (current.isEmpty()) {
            return;
        }

        PharmaTermsPolicy active = current.get();

        PharmaUserPolicyAcceptance acceptance = new PharmaUserPolicyAcceptance();
        acceptance.setUser(user);
        acceptance.setPolicy(active);
        acceptance.setOrganizationId(null); // no organization exists yet; backfilled later
        acceptance.setAccepted(Boolean.TRUE.equals(acceptedTerms));
        acceptance.setAcceptedAt(LocalDateTime.now());
        acceptance.setPolicyVersion(active.getVersion());
        acceptance.setIpAddress(ipAddress);

        acceptanceRepository.save(acceptance);
    }

    @Override
    public void backfillOrganization(String userId, Long organizationId) {
        if (userId == null || organizationId == null) {
            return;
        }
        acceptanceRepository.backfillOrganizationId(userId, organizationId);
    }

    private PharmaTermsPolicy requireActivePolicy() {
        return policyRepository.findFirstByStatus(PolicyStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException(
                        "No active terms & privacy policy has been published"));
    }

    /**
     * Version is unique, so this key is never reused and the object can be
     * treated as immutable once written.
     */
    private String buildDocumentKey(String version, MultipartFile file) {
        String safeVersion = version.replaceAll("[^A-Za-z0-9._-]", "_");

        String originalName = file.getOriginalFilename();
        String safeName = (originalName == null || originalName.isBlank())
                ? "terms"
                : originalName.replaceAll("[^A-Za-z0-9._-]", "_");

        return "legal/terms/" + safeVersion + "/" + safeName;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);

            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private TermsPolicyDto toDto(PharmaTermsPolicy policy) {
        TermsPolicyDto dto = new TermsPolicyDto();
        dto.setId(policy.getId());
        dto.setVersion(policy.getVersion());
        dto.setTitle(policy.getTitle());
        dto.setEffectiveFrom(policy.getEffectiveFrom());
        dto.setDocumentUrl(policy.getDocumentUrl());
        dto.setFileName(policy.getFileName());
        dto.setContentType(policy.getContentType());
        dto.setFileSize(policy.getFileSize());
        dto.setContentHash(policy.getContentHash());
        return dto;
    }

    private TermsPolicyAdminDto toAdminDto(PharmaTermsPolicy policy) {
        TermsPolicyAdminDto dto = new TermsPolicyAdminDto();
        dto.setId(policy.getId());
        dto.setVersion(policy.getVersion());
        dto.setTitle(policy.getTitle());
        dto.setEffectiveFrom(policy.getEffectiveFrom());
        dto.setDocumentUrl(policy.getDocumentUrl());
        dto.setFileName(policy.getFileName());
        dto.setContentType(policy.getContentType());
        dto.setFileSize(policy.getFileSize());
        dto.setContentHash(policy.getContentHash());
        dto.setStatus(policy.getStatus());
        dto.setRequiresReacceptance(policy.getRequiresReacceptance());
        dto.setCreatedAt(policy.getCreatedAt());
        dto.setCreatedBy(policy.getCreatedBy());
        return dto;
    }
}
