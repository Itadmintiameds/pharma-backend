package tiameds.pharmabackend.mapper;

import org.springframework.stereotype.Component;
import tiameds.pharmabackend.dto.PharmaDocumentsDto;
import tiameds.pharmabackend.entity.PharmaDocuments;

@Component
public class PharmaDocumentsMapper {

    public PharmaDocumentsDto toDto(PharmaDocuments entity) {
        if (entity == null) {
            return null;
        }

        PharmaDocumentsDto dto = new PharmaDocumentsDto();

        dto.setDocumentId(entity.getDocumentId());
        dto.setDocumentNo(entity.getDocumentNo());
        dto.setDocumentType(entity.getDocumentType());
        dto.setDocumentUrl(entity.getDocumentUrl());
        dto.setIssueDate(entity.getIssueDate());
        dto.setIssueAuthority(entity.getIssueAuthority());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setIsActive(entity.getIsActive());

        return dto;
    }

    public PharmaDocuments toEntity(PharmaDocumentsDto dto) {
        if (dto == null) {
            return null;
        }

        PharmaDocuments entity = new PharmaDocuments();

        entity.setDocumentId(dto.getDocumentId());
        entity.setDocumentNo(dto.getDocumentNo());
        entity.setDocumentType(dto.getDocumentType());
        entity.setDocumentUrl(dto.getDocumentUrl());
        entity.setIssueDate(dto.getIssueDate());
        entity.setIssueAuthority(dto.getIssueAuthority());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setIsActive(dto.getIsActive());

        return entity;
    }
}
