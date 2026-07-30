package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.DeviceSpecificationUnitDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.service.master.DeviceSpecificationUnitService;

import java.util.List;

@RestController
@RequestMapping("/master/device-specification-units")
@RequiredArgsConstructor
public class DeviceSpecificationUnitController {

    private final DeviceSpecificationUnitService deviceSpecificationUnitService;

    @GetMapping
    public ResponseEntity<List<DeviceSpecificationUnitDto>> getAllDeviceSpecificationUnits() {
        return ResponseEntity.ok(deviceSpecificationUnitService.getAllDeviceSpecificationUnits());
    }

    @GetMapping("/{deviceSpecificationUnitId}")
    public ResponseEntity<DeviceSpecificationUnitDto> getDeviceSpecificationUnitById(
            @PathVariable Long deviceSpecificationUnitId) {

        return ResponseEntity.ok(deviceSpecificationUnitService.getDeviceSpecificationUnitById(deviceSpecificationUnitId));
    }

    @PostMapping
    public ResponseEntity<DeviceSpecificationUnitDto> createDeviceSpecificationUnit(
            @RequestBody DeviceSpecificationUnitDto deviceSpecificationUnitDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(deviceSpecificationUnitService.createDeviceSpecificationUnit(deviceSpecificationUnitDto));
    }

    @PutMapping("/{deviceSpecificationUnitId}")
    public ResponseEntity<DeviceSpecificationUnitDto> updateDeviceSpecificationUnit(
            @PathVariable Long deviceSpecificationUnitId,
            @RequestBody DeviceSpecificationUnitDto deviceSpecificationUnitDto) {

        return ResponseEntity.ok(
                deviceSpecificationUnitService.updateDeviceSpecificationUnit(deviceSpecificationUnitId, deviceSpecificationUnitDto));
    }

    @PatchMapping("/{deviceSpecificationUnitId}/status")
    public ResponseEntity<DeviceSpecificationUnitDto> updateDeviceSpecificationUnitStatus(
            @PathVariable Long deviceSpecificationUnitId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                deviceSpecificationUnitService.updateDeviceSpecificationUnitStatus(
                        deviceSpecificationUnitId, request.getIsActive()));
    }
}
