package tiameds.pharmabackend.controller.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tiameds.pharmabackend.dto.billing.CustomerManagementDto;
import tiameds.pharmabackend.security.CustomUserDetails;
import tiameds.pharmabackend.service.billing.CustomerService;

import java.util.List;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/allCustomer")
    public ResponseEntity<?> getAllCustomers(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<CustomerManagementDto> customers =
                customerService.getAllCustomers(currentUser.getUser());

        return ResponseEntity.ok(customers);
    }


    @GetMapping("/phone/{customerPhoneNo}")
    public ResponseEntity<?> getCustomersByPhoneNo(
            @PathVariable String customerPhoneNo,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<CustomerManagementDto> customers = customerService.getCustomersByPhoneNo(
                customerPhoneNo,
                currentUser.getUser());

        return ResponseEntity.ok(customers);
    }
}
