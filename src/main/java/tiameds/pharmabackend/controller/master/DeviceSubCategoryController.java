package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.DeviceSubCategoryDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.service.master.DeviceSubCategoryService;

import java.util.List;

@RestController
@RequestMapping("/master/device-sub-categories")
@RequiredArgsConstructor
public class DeviceSubCategoryController {

    private final DeviceSubCategoryService deviceSubCategoryService;

    @GetMapping
    public ResponseEntity<List<DeviceSubCategoryDto>> getAllDeviceSubCategories() {
        return ResponseEntity.ok(deviceSubCategoryService.getAllDeviceSubCategories());
    }

    @GetMapping("/{deviceSubCategoryId}")
    public ResponseEntity<DeviceSubCategoryDto> getDeviceSubCategoryById(
            @PathVariable Long deviceSubCategoryId) {

        return ResponseEntity.ok(deviceSubCategoryService.getDeviceSubCategoryById(deviceSubCategoryId));
    }

    @PostMapping
    public ResponseEntity<DeviceSubCategoryDto> createDeviceSubCategory(
            @RequestBody DeviceSubCategoryDto deviceSubCategoryDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(deviceSubCategoryService.createDeviceSubCategory(deviceSubCategoryDto));
    }

    @PutMapping("/{deviceSubCategoryId}")
    public ResponseEntity<DeviceSubCategoryDto> updateDeviceSubCategory(
            @PathVariable Long deviceSubCategoryId,
            @RequestBody DeviceSubCategoryDto deviceSubCategoryDto) {

        return ResponseEntity.ok(
                deviceSubCategoryService.updateDeviceSubCategory(deviceSubCategoryId, deviceSubCategoryDto));
    }

    @PatchMapping("/{deviceSubCategoryId}/status")
    public ResponseEntity<DeviceSubCategoryDto> updateDeviceSubCategoryStatus(
            @PathVariable Long deviceSubCategoryId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                deviceSubCategoryService.updateDeviceSubCategoryStatus(
                        deviceSubCategoryId, request.getIsActive()));
    }
}
