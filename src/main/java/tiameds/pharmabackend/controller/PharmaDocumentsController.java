package tiameds.pharmabackend.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.service.PharmaDocumentsService;

@RestController
@RequestMapping("/pharmacyDocuments")
@RequiredArgsConstructor
public class PharmaDocumentsController {

    @PostConstruct
    public void init() {
        System.out.println("PharmaDocumentsController Loaded");
    }

    private final PharmaDocumentsService pharmaDocumentsService;

    @GetMapping("/checkDocument")
    public ResponseEntity<Boolean> checkDocumentNumber(
            @RequestParam String documentNo) {
        System.out.println("PharmaDocumentsController Loaded Oneeeeeeeeeeeeeeeeeeeeeee");

        return ResponseEntity.ok(
                pharmaDocumentsService.checkDocumentNumberExists(documentNo)
        );
    }


}