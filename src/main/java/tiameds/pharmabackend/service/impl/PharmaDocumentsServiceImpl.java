package tiameds.pharmabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.PharmaDocumentsService;
import tiameds.pharmabackend.repository.PharmaDocumentsRepository;

@Service
@RequiredArgsConstructor
public class PharmaDocumentsServiceImpl implements PharmaDocumentsService {

    private final PharmaDocumentsRepository pharmaDocumentsRepository;

    @Override
    public boolean checkDocumentNumberExists(String documentNo) {

        return pharmaDocumentsRepository.existsByDocumentNo(documentNo);
    }

}