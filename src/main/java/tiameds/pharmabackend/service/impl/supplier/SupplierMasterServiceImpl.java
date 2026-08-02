package tiameds.pharmabackend.service.impl.supplier;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.supplier.SupplierMasterDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.entity.supplier.SupplierMaster;
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
    private final CurrentPharmacyContext pharmacyContext;

    @Override
    public SupplierMasterDto createSupplier(
            SupplierMasterDto supplierDto,
            UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        SupplierMaster supplier = supplierMasterMapper.toEntity(supplierDto);

        supplier.setPharmacyId(pharmacyId);
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

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

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

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        SupplierMaster supplier = supplierMasterRepository
                .findBySupplierIdAndPharmacyId(supplierId, pharmacyId)
                .orElseThrow(() ->
                        new RuntimeException("Supplier not found"));

        return supplierMasterMapper.toDto(supplier);
    }
}