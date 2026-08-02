package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.FlavourDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.service.master.FlavourService;

import java.util.List;

@RestController
@RequestMapping("/master/flavours")
@RequiredArgsConstructor
public class FlavourController {

    private final FlavourService flavourService;

    @GetMapping
    public ResponseEntity<List<FlavourDto>> getAllFlavours() {
        return ResponseEntity.ok(flavourService.getAllFlavours());
    }

    @GetMapping("/{flavourId}")
    public ResponseEntity<FlavourDto> getFlavourById(@PathVariable Long flavourId) {
        return ResponseEntity.ok(flavourService.getFlavourById(flavourId));
    }

    @PostMapping
    public ResponseEntity<FlavourDto> createFlavour(@RequestBody FlavourDto flavourDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(flavourService.createFlavour(flavourDto));
    }

    @PutMapping("/{flavourId}")
    public ResponseEntity<FlavourDto> updateFlavour(
            @PathVariable Long flavourId,
            @RequestBody FlavourDto flavourDto) {

        return ResponseEntity.ok(flavourService.updateFlavour(flavourId, flavourDto));
    }

    @PatchMapping("/{flavourId}/status")
    public ResponseEntity<FlavourDto> updateFlavourStatus(
            @PathVariable Long flavourId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                flavourService.updateFlavourStatus(flavourId, request.getIsActive()));
    }
}
