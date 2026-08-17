package tiameds.pharmabackend.service.impl.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.pharmabackend.dto.warehouse.WarehouseDto;
import tiameds.pharmabackend.dto.warehouse.WarehousePharmacyAssignmentDto;
import tiameds.pharmabackend.entity.PharmacyDetails;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.warehouse.Warehouse;
import tiameds.pharmabackend.mapper.warehouse.WarehouseMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.warehouse.WarehouseRepository;
import tiameds.pharmabackend.service.PharmacyOrganizationService;
import tiameds.pharmabackend.service.warehouse.WarehouseService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;
    private final PharmacyOrganizationService organizationService;
    private final WarehouseIdGenerator warehouseIdGenerator;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;

    @Override
    public WarehouseDto createWarehouse(WarehouseDto warehouseDto, UserDetails user) {

        PharmacyOrganization organization =
                organizationService.getUserOrganization(user.getUserId());

        Warehouse warehouse = warehouseMapper.toEntity(warehouseDto);

        warehouse.setOrganization(organization);
        warehouse.setWarehouseId(warehouseIdGenerator.generate(
                organization.getOrganizationName(), warehouse.getWarehouseName()));
        warehouse.setCreatedBy(String.valueOf(user.getUserId()));
        warehouse.setCreatedAt(LocalDateTime.now());
        if (warehouse.getIsActive() == null) {
            warehouse.setIsActive(Boolean.TRUE);
        }

        Warehouse saved = warehouseRepository.save(warehouse);

        return warehouseMapper.toDto(saved);
    }

    @Override
    public WarehouseDto updateWarehouse(String warehouseId, WarehouseDto warehouseDto, UserDetails user) {

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + warehouseId));

        PharmacyOrganization organization =
                organizationService.getUserOrganization(user.getUserId());

        if (warehouse.getOrganization() == null
                || !warehouse.getOrganization().getOrganizationId().equals(organization.getOrganizationId())) {
            throw new RuntimeException("You are not authorized to update this warehouse.");
        }

        warehouse.setWarehouseName(warehouseDto.getWarehouseName());
        warehouse.setWarehouseCode(warehouseDto.getWarehouseCode());
        warehouse.setWarehouseAddress(warehouseDto.getWarehouseAddress());
        warehouse.setContactPersonName(warehouseDto.getContactPersonName());
        warehouse.setMobileNumber(warehouseDto.getMobileNumber());
        if (warehouseDto.getIsActive() != null) {
            warehouse.setIsActive(warehouseDto.getIsActive());
        }
        warehouse.setModifiedBy(String.valueOf(user.getUserId()));
        warehouse.setModifiedAt(LocalDateTime.now());

        Warehouse saved = warehouseRepository.save(warehouse);

        return warehouseMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDto getWarehouseById(String warehouseId) {

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + warehouseId));

        return warehouseMapper.toDto(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDto> getWarehousesForUser(UserDetails user) {

        PharmacyOrganization organization =
                organizationService.getUserOrganization(user.getUserId());

        return warehouseRepository
                .findAllByOrganization_OrganizationIdOrderByWarehouseName(organization.getOrganizationId())
                .stream()
                .map(warehouseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDto> getWarehousesByOrganizationId(Long organizationId, UserDetails user) {

        PharmacyOrganization organization =
                organizationService.getUserOrganization(user.getUserId());

        if (!organization.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("You are not authorized to access warehouses of this organization.");
        }

        return warehouseRepository
                .findAllByOrganization_OrganizationIdOrderByWarehouseName(organizationId)
                .stream()
                .map(warehouseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public WarehousePharmacyAssignmentDto assignPharmacies(
            String warehouseId, List<String> pharmacyIds, UserDetails user) {

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + warehouseId));

        PharmacyOrganization organization =
                organizationService.getUserOrganization(user.getUserId());

        if (warehouse.getOrganization() == null
                || !warehouse.getOrganization().getOrganizationId().equals(organization.getOrganizationId())) {
            throw new RuntimeException("You are not authorized to modify this warehouse.");
        }

        if (pharmacyIds == null || pharmacyIds.isEmpty()) {
            throw new RuntimeException("At least one pharmacy id is required");
        }

        Set<String> uniqueIds = new LinkedHashSet<>(pharmacyIds);
        uniqueIds.remove(null);

        if (uniqueIds.isEmpty()) {
            throw new RuntimeException("At least one pharmacy id is required");
        }

        Map<String, PharmacyDetails> pharmaciesById = pharmacyDetailsRepository
                .findAllById(uniqueIds)
                .stream()
                .collect(Collectors.toMap(PharmacyDetails::getPharmacyId, Function.identity()));

        List<PharmacyDetails> toAssign = new ArrayList<>();

        for (String pharmacyId : uniqueIds) {

            PharmacyDetails pharmacy = pharmaciesById.get(pharmacyId);

            if (pharmacy == null) {
                throw new RuntimeException("Pharmacy not found with id : " + pharmacyId);
            }

            if (pharmacy.getOrganization() == null
                    || !organization.getOrganizationId().equals(
                            pharmacy.getOrganization().getOrganizationId())) {
                throw new RuntimeException(
                        "Pharmacy does not belong to your organization : " + pharmacyId);
            }

            pharmacy.setWarehouse(warehouse);
            toAssign.add(pharmacy);
        }

        pharmacyDetailsRepository.saveAll(toAssign);

        return new WarehousePharmacyAssignmentDto(warehouseId, new ArrayList<>(uniqueIds));
    }

    @Override
    public void deleteWarehouse(String warehouseId) {

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + warehouseId));

        warehouseRepository.delete(warehouse);
    }
}
