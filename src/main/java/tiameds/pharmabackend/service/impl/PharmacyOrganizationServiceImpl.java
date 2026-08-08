package tiameds.pharmabackend.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.dto.PharmacyOrganizationDto;
import tiameds.pharmabackend.dto.WarehouseDto;
import tiameds.pharmabackend.entity.PharmacyOrganization;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.Warehouse;
import tiameds.pharmabackend.mapper.PharmacyOrganizationMapper;
import tiameds.pharmabackend.mapper.WarehouseMapper;
import tiameds.pharmabackend.repository.PharmacyOrganizationRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.WarehouseRepository;
import tiameds.pharmabackend.service.PharmacyOrganizationService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PharmacyOrganizationServiceImpl implements PharmacyOrganizationService {

    private final PharmacyOrganizationRepository organizationRepository;
    private final PharmacyOrganizationMapper organizationMapper;
    private final UserDetailsRepository userDetailsRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    @Override
    @Transactional
    public PharmacyOrganizationDto createOrganization(
            PharmacyOrganizationDto organizationDto,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PharmacyOrganization organization =
                organizationMapper.toEntity(organizationDto);

        organization.setCreatedAt(LocalDateTime.now());
        organization.setIsRejected(Boolean.FALSE);
        organization.setIsActive(Boolean.TRUE);

        PharmacyOrganization savedOrganization =
                organizationRepository.save(organization);

        // Associate the logged-in user with this organization
        persistentUser.setOrganization(savedOrganization);

        userDetailsRepository.save(persistentUser);

        // Flow 3: centrally-managed organization -> create the central warehouse(s) using
        // the details sent from the frontend. Products will be purchased into this
        // warehouse and later transferred to pharmacies.
        // if (Boolean.TRUE.equals(savedOrganization.getCentralizedInventory())) {
        //     createCentralWarehouse(organizationDto.getWarehouse(), savedOrganization, persistentUser);
        // }
        if (Boolean.TRUE.equals(savedOrganization.getCentralizedInventory())) {
            List<WarehouseDto> warehouses = organizationDto.getWarehouses();
            if (warehouses == null || warehouses.isEmpty()) {
                // No warehouse details sent: still create one central warehouse for flow 3.
                createCentralWarehouse(null, savedOrganization, persistentUser);
            } else {
                for (WarehouseDto warehouseDto : warehouses) {
                    createCentralWarehouse(warehouseDto, savedOrganization, persistentUser);
                }
            }
        }

        return organizationMapper.toDto(savedOrganization);
    }

    private void createCentralWarehouse(WarehouseDto warehouseDto,
                                        PharmacyOrganization organization,
                                        UserDetails user) {

        // Map the warehouse details supplied by the frontend; fall back to an empty
        // warehouse when none were sent so a central warehouse still exists for flow 3.
        Warehouse warehouse = warehouseMapper.toEntity(warehouseDto);
        if (warehouse == null) {
            warehouse = new Warehouse();
        }

        // Never trust an id/organization from the payload for a fresh warehouse.
        warehouse.setWarehouseId(null);
        warehouse.setOrganization(organization);

        if (warehouse.getWarehouseName() == null || warehouse.getWarehouseName().isBlank()) {
            String orgName = organization.getOrganizationName() == null
                    ? "Organization" : organization.getOrganizationName();
            warehouse.setWarehouseName(orgName + " Central Warehouse");
        }

        if (warehouse.getIsActive() == null) {
            warehouse.setIsActive(Boolean.TRUE);
        }

        warehouse.setCreatedBy(String.valueOf(user.getUserId()));
        warehouse.setCreatedAt(LocalDateTime.now());

        warehouseRepository.save(warehouse);

        log.info("Central warehouse created for organization {}", organization.getOrganizationId());
    }

    @Override
    public void rejectRequest(Long userId) {

        UserDetails user = userDetailsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PharmacyOrganization organization = user.getOrganization();

        boolean userHasPharmacy =
                user.getPharmacies() != null && !user.getPharmacies().isEmpty();

        boolean organizationHasPharmacy =
                organization != null
                        && organization.getPharmacies() != null
                        && !organization.getPharmacies().isEmpty();

        // A pharmacy already exists for this user/organization: they remain active,
        // only the individual registration is rejected on the admin side.
        if (userHasPharmacy || organizationHasPharmacy) {
            log.info("Skipping reject for user {}: user/organization already has pharmacies", userId);
            return;
        }

        user.setIsRejected(Boolean.TRUE);
        user.setModifiedAt(LocalDateTime.now());

        userDetailsRepository.save(user);

        if (organization != null) {
            organization.setIsRejected(Boolean.TRUE);
            organizationRepository.save(organization);
        }
    }

    @Override
    public PharmacyOrganization getUserOrganization(Long userId) {

        UserDetails user = userDetailsRepository.findByUserIdWithOrganization(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOrganization() == null) {
            throw new RuntimeException("Organization not found for this user.");
        }

        return user.getOrganization();
    }
}