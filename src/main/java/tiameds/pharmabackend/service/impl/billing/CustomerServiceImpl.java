package tiameds.pharmabackend.service.impl.billing;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tiameds.pharmabackend.context.CurrentPharmacyContext;
import tiameds.pharmabackend.dto.billing.CustomerManagementDto;
import tiameds.pharmabackend.entity.UserDetails;
import tiameds.pharmabackend.mapper.billing.CustomerManagementMapper;
import tiameds.pharmabackend.repository.PharmacyDetailsRepository;
import tiameds.pharmabackend.repository.UserDetailsRepository;
import tiameds.pharmabackend.repository.billing.CustomerManagementRepository;
import tiameds.pharmabackend.service.billing.CustomerService;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerManagementRepository customerManagementRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PharmacyDetailsRepository pharmacyDetailsRepository;
    private final CurrentPharmacyContext pharmacyContext;


    @Override
    public List<CustomerManagementDto> getAllCustomers(UserDetails user) {

        String pharmacyId = resolvePharmacyId(user);

        return customerManagementRepository.findByPharmacy_PharmacyId(pharmacyId)
                .stream()
                .map(CustomerManagementMapper::toDto)
                .collect(Collectors.toList());
    }


    @Override
    public List<CustomerManagementDto> getCustomersByPhoneNo(
            String customerPhoneNo,
            UserDetails user) {

        String pharmacyId = resolvePharmacyId(user);

        if (customerPhoneNo == null || customerPhoneNo.isBlank()) {
            throw new RuntimeException("Customer phone number is required");
        }

        return customerManagementRepository
                .findByPharmacy_PharmacyIdAndCustomerPhoneNo(pharmacyId, customerPhoneNo.trim())
                .stream()
                .map(CustomerManagementMapper::toDto)
                .collect(Collectors.toList());
    }


    private String resolvePharmacyId(UserDetails user) {

        UserDetails persistentUser = userDetailsRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String pharmacyId = pharmacyContext.getCurrentPharmacy();

        boolean valid = pharmacyDetailsRepository.existsUserPharmacy(
                pharmacyId,
                persistentUser.getUserId());

        if (!valid) {
            throw new RuntimeException("You are not authorized to use this pharmacy.");
        }

        return pharmacyId;
    }
}
