package tiameds.pharmabackend.service.impl.supplier;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.context.LocationContext;
import tiameds.pharmabackend.context.LocationContextResolver;
import tiameds.pharmabackend.dto.supplier.SupplierDuplicateCheckResponse;
import tiameds.pharmabackend.dto.supplier.SupplierMasterDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;
import tiameds.pharmabackend.enums.SupplierStatus;
import tiameds.pharmabackend.mapper.supplier.SupplierMasterMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.supplier.SupplierMasterRepository;
import tiameds.pharmabackend.service.supplier.SupplierMasterService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierMasterServiceImpl implements SupplierMasterService {

    private final SupplierMasterRepository supplierMasterRepository;
    private final SupplierMasterMapper supplierMasterMapper;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final LocationContextResolver locationContextResolver;

    @Override
    public SupplierMasterDto createSupplier(
            SupplierMasterDto supplierDto,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        SupplierMaster supplier = supplierMasterMapper.toEntity(supplierDto);

        if (location.isWarehouse()) {
            // Warehouse supplier: scoped to the manager's warehouse (already authorized
            // because the warehouse is derived from the user's own binding).
            supplier.setPharmacyId(null);
            supplier.setWarehouseId(location.getLocationId());
        } else {
            String pharmacyId = location.getLocationId();
            boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                    pharmacyId,
                    persistentUser.getUserId());
            if (!valid) {
                throw new RuntimeException("You are not authorized to use this pharmacy.");
            }
            supplier.setPharmacyId(pharmacyId);
            supplier.setWarehouseId(null);
        }

        validateNoDuplicateSupplier(supplierDto, location, null);

        supplier.setStatus(SupplierStatus.ACTIVE);

        supplier.setCreatedBy(String.valueOf(persistentUser.getUserId()));
        supplier.setCreatedAt(LocalDateTime.now());

        supplier.setModifiedBy(null);
        supplier.setModifiedAt(null);

        SupplierMaster savedSupplier =
                supplierMasterRepository.save(supplier);

        return supplierMasterMapper.toDto(savedSupplier);
    }


    @Override
    public List<SupplierMasterDto> getAllSuppliers(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isWarehouse()) {
            return supplierMasterRepository.findByWarehouseId(location.getLocationId())
                    .stream()
                    .map(supplierMasterMapper::toDto)
                    .collect(Collectors.toList());
        }

        String pharmacyId = location.getLocationId();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return supplierMasterRepository.findByPharmacyId(pharmacyId)
                .stream()
                .map(supplierMasterMapper::toDto)
                .collect(Collectors.toList());
    }


    @Override
    public SupplierMasterDto getSupplierById(
            Long supplierId,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        SupplierMaster supplier;

        if (location.isWarehouse()) {
            supplier = supplierMasterRepository
                    .findBySupplierIdAndWarehouseId(supplierId, location.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Supplier not found"));
        } else {
            String pharmacyId = location.getLocationId();

            boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                    pharmacyId,
                    persistentUser.getUserId());

            if (!valid) {
                throw new RuntimeException("You are not authorized to use this pharmacy.");
            }

            supplier = supplierMasterRepository
                    .findBySupplierIdAndPharmacyId(supplierId, pharmacyId)
                    .orElseThrow(() -> new RuntimeException("Supplier not found"));
        }

        return supplierMasterMapper.toDto(supplier);
    }


    @Override
    public SupplierMasterDto updateSupplier(
            Long supplierId,
            SupplierMasterDto supplierDto,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        SupplierMaster supplier;

        if (location.isWarehouse()) {
            supplier = supplierMasterRepository
                    .findBySupplierIdAndWarehouseId(supplierId, location.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Supplier not found"));
        } else {
            String pharmacyId = location.getLocationId();

            boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                    pharmacyId,
                    persistentUser.getUserId());

            if (!valid) {
                throw new RuntimeException("You are not authorized to use this pharmacy.");
            }

            supplier = supplierMasterRepository
                    .findBySupplierIdAndPharmacyId(supplierId, pharmacyId)
                    .orElseThrow(() -> new RuntimeException("Supplier not found"));
        }

        validateNoDuplicateSupplier(supplierDto, location, supplierId);

        supplier.setSupplierName(supplierDto.getSupplierName());
        supplier.setDlno(supplierDto.getDlno());
        supplier.setGstinNo(supplierDto.getGstinNo());
        supplier.setPanNo(supplierDto.getPanNo());
        supplier.setDlExpiryDate(supplierDto.getDlExpiryDate());
        supplier.setIssuingAuthority(supplierDto.getIssuingAuthority());
        supplier.setFssaiNo(supplierDto.getFssaiNo());
        supplier.setContactPersonName(supplierDto.getContactPersonName());
        supplier.setMobileNumber(supplierDto.getMobileNumber());
        supplier.setSupplierEmail(supplierDto.getSupplierEmail());
        supplier.setAddress(supplierDto.getAddress());
        supplier.setBuildingNo(supplierDto.getBuildingNo());
        supplier.setPincode(supplierDto.getPincode());
        supplier.setCity(supplierDto.getCity());
        supplier.setDistrict(supplierDto.getDistrict());
        supplier.setState(supplierDto.getState());
        supplier.setBankName(supplierDto.getBankName());
        supplier.setAccountHolderName(supplierDto.getAccountHolderName());
        supplier.setAccountNumber(supplierDto.getAccountNumber());
        supplier.setIfscCode(supplierDto.getIfscCode());

        if (supplierDto.getStatus() != null) {
            supplier.setStatus(supplierDto.getStatus());
        }

        supplier.setModifiedBy(String.valueOf(persistentUser.getUserId()));
        supplier.setModifiedAt(LocalDateTime.now());

        SupplierMaster updatedSupplier =
                supplierMasterRepository.save(supplier);

        return supplierMasterMapper.toDto(updatedSupplier);
    }


    @Override
    public SupplierDuplicateCheckResponse checkDuplicateSupplier(
            SupplierMasterDto supplierDto,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocationContext location = locationContextResolver.resolve(persistentUser);

        if (location.isPharmacy()) {

            boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                    location.getLocationId(),
                    persistentUser.getUserId());

            if (!valid) {
                throw new RuntimeException("You are not authorized to use this pharmacy.");
            }
        }

        return findDuplicateSupplier(supplierDto, location, supplierDto.getSupplierId());
    }

    // Two suppliers of the same pharmacy or warehouse cannot share a GST, DL,
    // PAN or FSSAI number.
    private void validateNoDuplicateSupplier(
            SupplierMasterDto supplierDto,
            LocationContext location,
            Long excludeSupplierId) {

        SupplierDuplicateCheckResponse check =
                findDuplicateSupplier(supplierDto, location, excludeSupplierId);

        if (check.isDuplicate()) {
            throw new RuntimeException(String.join("; ", check.getMessages()));
        }
    }

    private SupplierDuplicateCheckResponse findDuplicateSupplier(
            SupplierMasterDto supplierDto,
            LocationContext location,
            Long excludeSupplierId) {

        String gstinNo = normalize(supplierDto.getGstinNo());
        String dlno = normalize(supplierDto.getDlno());
        String panNo = normalize(supplierDto.getPanNo());
        String fssaiNo = normalize(supplierDto.getFssaiNo());

        SupplierDuplicateCheckResponse response = new SupplierDuplicateCheckResponse();

        if (gstinNo == null && dlno == null && panNo == null && fssaiNo == null) {
            return response;
        }

        // -1 never matches a real id, so nothing is excluded on create.
        Long excludeId = excludeSupplierId != null ? excludeSupplierId : -1L;

        List<SupplierMaster> matches = location.isWarehouse()
                ? supplierMasterRepository.findDuplicatesInWarehouse(
                        location.getLocationId(), excludeId, gstinNo, dlno, panNo, fssaiNo)
                : supplierMasterRepository.findDuplicatesInPharmacy(
                        location.getLocationId(), excludeId, gstinNo, dlno, panNo, fssaiNo);

        for (SupplierMaster match : matches) {

            if (gstinNo != null && gstinNo.equals(normalize(match.getGstinNo())) && !response.isGstinNoExists()) {
                response.setGstinNoExists(true);
                response.getMessages().add("GST number already exists for supplier " + match.getSupplierName());
            }

            if (dlno != null && dlno.equals(normalize(match.getDlno())) && !response.isDlnoExists()) {
                response.setDlnoExists(true);
                response.getMessages().add("DL number already exists for supplier " + match.getSupplierName());
            }

            if (panNo != null && panNo.equals(normalize(match.getPanNo())) && !response.isPanNoExists()) {
                response.setPanNoExists(true);
                response.getMessages().add("PAN number already exists for supplier " + match.getSupplierName());
            }

            if (fssaiNo != null && fssaiNo.equals(normalize(match.getFssaiNo())) && !response.isFssaiNoExists()) {
                response.setFssaiNoExists(true);
                response.getMessages().add("FSSAI number already exists for supplier " + match.getSupplierName());
            }
        }

        response.setDuplicate(!response.getMessages().isEmpty());

        return response;
    }

    // Trimmed and upper-cased so "29abcde1234f1z5 " and "29ABCDE1234F1Z5" match;
    // blank values are treated as not provided.
    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toUpperCase();
    }
}