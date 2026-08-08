package tiameds.pharmabackend.service.billing;

import tiameds.pharmabackend.dto.billing.CustomerManagementDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface CustomerService {

    List<CustomerManagementDto> getAllCustomers(UserDetails user);

    // One number can carry several customers, so every match is returned
    List<CustomerManagementDto> getCustomersByPhoneNo(String customerPhoneNo, UserDetails user);
}
