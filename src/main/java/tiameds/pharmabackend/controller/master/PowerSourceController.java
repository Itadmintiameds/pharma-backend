package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.dto.master.PowerSourceDto;
import tiameds.pharmabackend.service.master.PowerSourceService;

import java.util.List;

@RestController
@RequestMapping("/master/power-sources")
@RequiredArgsConstructor
public class PowerSourceController {

    private final PowerSourceService powerSourceService;

    @GetMapping
    public ResponseEntity<List<PowerSourceDto>> getAllPowerSources() {
        return ResponseEntity.ok(powerSourceService.getAllPowerSources());
    }

    @GetMapping("/{powerSourceId}")
    public ResponseEntity<PowerSourceDto> getPowerSourceById(@PathVariable Long powerSourceId) {
        return ResponseEntity.ok(powerSourceService.getPowerSourceById(powerSourceId));
    }

    @PostMapping
    public ResponseEntity<PowerSourceDto> createPowerSource(@RequestBody PowerSourceDto powerSourceDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(powerSourceService.createPowerSource(powerSourceDto));
    }

    @PutMapping("/{powerSourceId}")
    public ResponseEntity<PowerSourceDto> updatePowerSource(
            @PathVariable Long powerSourceId,
            @RequestBody PowerSourceDto powerSourceDto) {

        return ResponseEntity.ok(powerSourceService.updatePowerSource(powerSourceId, powerSourceDto));
    }

    @PatchMapping("/{powerSourceId}/status")
    public ResponseEntity<PowerSourceDto> updatePowerSourceStatus(
            @PathVariable Long powerSourceId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                powerSourceService.updatePowerSourceStatus(powerSourceId, request.getIsActive()));
    }
}
