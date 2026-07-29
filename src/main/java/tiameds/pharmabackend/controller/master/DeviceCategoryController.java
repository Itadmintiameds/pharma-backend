package tiameds.pharmabackend.controller.master;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.master.DeviceCategoryDto;
import tiameds.pharmabackend.dto.master.DeviceSubCategoryDto;
import tiameds.pharmabackend.dto.master.MasterStatusDto;
import tiameds.pharmabackend.service.master.DeviceCategoryService;
import tiameds.pharmabackend.service.master.DeviceSubCategoryService;

import java.util.List;

@RestController
@RequestMapping("/master/device-categories")
@RequiredArgsConstructor
public class DeviceCategoryController {

    private final DeviceCategoryService deviceCategoryService;
    private final DeviceSubCategoryService deviceSubCategoryService;

    @GetMapping
    public ResponseEntity<List<DeviceCategoryDto>> getAllDeviceCategories() {
        return ResponseEntity.ok(deviceCategoryService.getAllDeviceCategories());
    }

    @GetMapping("/{deviceCategoryId}")
    public ResponseEntity<DeviceCategoryDto> getDeviceCategoryById(@PathVariable Long deviceCategoryId) {
        return ResponseEntity.ok(deviceCategoryService.getDeviceCategoryById(deviceCategoryId));
    }

    @GetMapping("/{deviceCategoryId}/device-sub-categories")
    public ResponseEntity<List<DeviceSubCategoryDto>> getDeviceSubCategoriesByDeviceCategoryId(
            @PathVariable Long deviceCategoryId) {

        return ResponseEntity.ok(
                deviceSubCategoryService.getDeviceSubCategoriesByDeviceCategoryId(deviceCategoryId));
    }

    @PostMapping
    public ResponseEntity<DeviceCategoryDto> createDeviceCategory(@RequestBody DeviceCategoryDto deviceCategoryDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(deviceCategoryService.createDeviceCategory(deviceCategoryDto));
    }

    @PutMapping("/{deviceCategoryId}")
    public ResponseEntity<DeviceCategoryDto> updateDeviceCategory(
            @PathVariable Long deviceCategoryId,
            @RequestBody DeviceCategoryDto deviceCategoryDto) {

        return ResponseEntity.ok(deviceCategoryService.updateDeviceCategory(deviceCategoryId, deviceCategoryDto));
    }

    @PatchMapping("/{deviceCategoryId}/status")
    public ResponseEntity<DeviceCategoryDto> updateDeviceCategoryStatus(
            @PathVariable Long deviceCategoryId,
            @RequestBody MasterStatusDto request) {

        return ResponseEntity.ok(
                deviceCategoryService.updateDeviceCategoryStatus(deviceCategoryId, request.getIsActive()));
    }
}
