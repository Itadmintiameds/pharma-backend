package tiameds.pharmabackend.service.billing;

import org.springframework.web.multipart.MultipartFile;
import tiameds.pharmabackend.dto.billing.BillingDto;
import tiameds.pharmabackend.dto.billing.BillingPaymentDto;
import tiameds.pharmabackend.dto.billing.PrescriptionUploadDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface BillingService {

    BillingDto createBilling(BillingDto billingDto, UserDetails user);

    List<BillingDto> getAllBillings(UserDetails user);

    BillingDto getBillingById(Long billingId, UserDetails user);

    BillingDto updateBilling(Long billingId, BillingDto billingDto, UserDetails user);

    void deleteBilling(Long billingId, UserDetails user);

    // Records one more receipt against an existing bill and re-derives its status
    BillingDto addPayment(Long billingId, BillingPaymentDto paymentDto, UserDetails user);

    PrescriptionUploadDto uploadPrescription(Long billingId, MultipartFile file, UserDetails user);
}
