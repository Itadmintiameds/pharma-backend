package tiameds.pharmabackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.service.PharmaDocumentsService;

@RestController
@RequestMapping("/pharmacyDocuments")
@RequiredArgsConstructor
public class PharmaDocumentsController {

    private final PharmaDocumentsService pharmaDocumentsService;

    @GetMapping("/checkDocument")
    public ResponseEntity<Boolean> checkDocumentNumber(
            @RequestParam String documentNo) {
        return ResponseEntity.ok(
                pharmaDocumentsService.checkDocumentNumberExists(documentNo)
        );
    }


}